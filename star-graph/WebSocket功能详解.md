# WebSocket双向通信功能详解

## 📋 总览

本项目中WebSocket实现了**双向通信**架构：

- **作为客户端**：连接ComfyUI服务接收任务进度推送
- **作为服务端**：基于STOMP协议向前端推送实时消息

---

## 🔄 WebSocket双向通信架构

```text
┌─────────────────────────────────────────────────────────────────┐
│                     前端（Vue3）                                 │
│               WebSocket客户端（STOMP.js）                        │
│          订阅: /user/topic/messages                              │
└───────────────────────┬─────────────────────────────────────────┘
                        │ STOMP over WebSocket
                        │ ws://host:8080/ws
                        │
┌───────────────────────▼─────────────────────────────────────────┐
│                Spring Boot 应用（星图项目）                      │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │           WebSocket服务端（STOMP协议）                     │ │
│  │  - WebSocketConfig: 配置STOMP端点和消息代理               │ │
│  │  - WsNoticeService: 推送消息给前端                        │ │
│  │  - WebscoketUserInterceptor: 提取clientId身份标识         │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │          WebSocket客户端（连接ComfyUI）                    │ │
│  │  - ComfyuiConfig: 建立WebSocket连接                       │ │
│  │  - ComfyuiMessageHandler: 接收和处理消息                  │ │
│  │  - ComfyuiMessageService: 业务逻辑处理                    │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                  │
└───────────────────────┬─────────────────────────────────────────┘
                        │ WebSocket Client
                        │ ws://192.168.100.129:8188/ws
                        │
┌───────────────────────▼─────────────────────────────────────────┐
│                   ComfyUI AI服务                                 │
│          推送: progress、executed、execution_error消息           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎯 WebSocket在项目中的功能

### 功能一：作为服务端向前端推送消息（STOMP协议）

#### 1.1 技术实现

**核心配置类：** `WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    // 1. 注册WebSocket连接端点
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")  // 端点路径
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");  // 允许跨域
    }
    
    // 2. 配置消息代理
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic: 广播消息（一对多）
        // /user: 点对点消息（一对一）
        registry.enableSimpleBroker("/topic", "/user");
    }
    
    // 3. 配置用户身份拦截器
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebscoketUserInterceptor());
    }
}
```

**用户身份拦截器：** `WebscoketUserInterceptor.java`

- 从CONNECT消息头提取`clientId`
- 将`clientId`设置为`Principal`用户身份
- 支持点对点消息推送：`convertAndSendToUser(clientId, ...)`

**消息推送服务：** `WsNoticeServiceImpl.java`

```java
@Service
public class WsNoticeServiceImpl implements WsNoticeService {
    public final static String COMFYUI_QUEUE_TOPIC = "/topic/messages";
    
    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;
    
    // 点对点推送（只有指定clientId的用户能收到）
    @Override
    public void sendToUser(String clientId, String message) {
        simpMessagingTemplate.convertAndSendToUser(clientId, COMFYUI_QUEUE_TOPIC, message);
    }
    
    // 广播推送（所有订阅的用户都能收到）
    @Override
    public void sendToAll(String message) {
        simpMessagingTemplate.convertAndSend(COMFYUI_QUEUE_TOPIC, message);
    }
}
```

#### 1.2 推送的消息类型

| 消息类型 | 触发时机 | 消息内容 | 前端用途 |
|---------|---------|---------|---------|
| **progress** | ComfyUI任务执行中 | `{"type":"progress","value":5,"max":20}` | 显示进度条 |
| **imageResult** | ComfyUI生成完成 | `{"type":"imageResult","urls":["http://..."]}` | 展示生成的图片 |
| **execution_error** | ComfyUI执行失败 | `{"type":"execution_error","error":"..."}` | 显示错误提示 |

#### 1.3 前端连接示例

```javascript
// 使用SockJS + STOMP.js
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// 连接时携带clientId
const headers = { clientId: 'user-123' };

stompClient.connect(headers, function(frame) {
    console.log('Connected: ' + frame);
    
    // 订阅个人消息主题
    stompClient.subscribe('/user/topic/messages', function(message) {
        const data = JSON.parse(message.body);
        
        if (data.type === 'progress') {
            // 更新进度条
            updateProgress(data.value, data.max);
        } else if (data.type === 'imageResult') {
            // 显示生成的图片
            displayImages(data.urls);
        } else if (data.type === 'execution_error') {
            // 显示错误信息
            showError(data.error);
        }
    });
});
```

---

### 功能二：作为客户端连接ComfyUI接收任务进度

#### 2.1 技术实现

**配置类：** `ComfyuiConfig.java`

```java
@Configuration
public class ComfyuiConfig {
    
    @Bean
    public WebSocketConnectionManager webSocketConnectionManager(
            ComfyuiMessageHandler comfyuiMessageHandler) {
        
        // 1. 创建WebSocket客户端
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        
        // 2. 构造连接URL（携带clientId）
        String url = "ws://192.168.100.129:8188/ws?clientId=" + 
                     Constants.COMFYUI_CLIENT_ID;
        
        // 3. 创建连接管理器
        WebSocketConnectionManager manager = new WebSocketConnectionManager(
            webSocketClient, 
            comfyuiMessageHandler, 
            url
        );
        
        // 4. 启动连接（应用启动时自动建立）
        manager.start();
        
        return manager;
    }
}
```

**消息处理器：** `ComfyuiMessageHandler.java`

```java
@Component
public class ComfyuiMessageHandler extends TextWebSocketHandler {
    
    @Autowired
    ComfyuiMessageService comfyuiMessageService;
    
    // 连接成功回调
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("=============连接ComfyUI成功");
    }
    
    // 接收消息回调
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 1. 提取消息内容
        String payload = message.getPayload();
        
        // 2. 解析为MessageBase对象
        MessageBase messageBase = JSON.parseObject(payload, MessageBase.class);
        
        // 3. 委托给业务层处理
        comfyuiMessageService.handleMessage(messageBase);
    }
}
```

#### 2.2 接收的消息类型

**消息类型一：status（ComfyUI队列状态）**

```json
{
  "type": "status",
  "data": {
    "status": {
      "exec_info": {
        "queue_remaining": 0  // 队列剩余任务数
      }
    }
  }
}
```

**处理逻辑：**
- 当`queue_remaining == 0`时，说明ComfyUI队列已清空
- 释放Redisson信号量，允许定时任务继续提交新任务

---

#### 消息类型二：progress（任务执行进度）

```json
{
  "type": "progress",
  "data": {
    "value": 5,          // 当前步数
    "max": 20,           // 总步数
    "prompt_id": "xxx",  // ComfyUI任务ID
    "node": "4"          // 当前执行节点
  }
}
```

#### 处理逻辑：

1. 根据`prompt_id`从Redis获取任务信息（包含`wsClientId`）
2. 通过`WsNoticeService.sendToUser()`转发给前端
3. 前端显示实时进度条

---

#### 消息类型三：executed（任务执行完成）

```json
{
  "type": "executed",
  "data": {
    "prompt_id": "xxx",
    "output": {
      "images": [
        {
          "filename": "xxx.png",
          "type": "output",
          "subfolder": ""
        }
      ]
    }
  }
}
```

#### 处理逻辑：

1. 提取图片元数据，构造图片访问URL
2. 保存图片URL到数据库（用户历史记录）
3. 推送`imageResult`消息给前端展示图片

---

#### 消息类型四：execution_error（任务执行失败）

```json
{
  "type": "execution_error",
  "data": {
    "prompt_id": "xxx",
    "error": "错误详情"
  }
}
```

#### 处理逻辑：

1. 根据`prompt_id`获取任务信息
2. 归还用户冻结的积分
3. 推送错误消息给前端显示

---

## 🔥 核心业务逻辑

### ComfyUI消息处理服务：`ComfyuiMessageServiceImpl.java`

```java
@Service
public class ComfyuiMessageServiceImpl implements ComfyuiMessageService {
    
    @Autowired
    WsNoticeService wsNoticeService;
    @Autowired
    RedisService redisService;
    @Autowired
    RedissonClient redissonClient;
    
    @Override
    public void handleMessage(MessageBase messageBase) {
        // 根据消息类型分发处理
        if ("progress".equals(messageBase.getType())) {
            handleProgressMessage(messageBase);
        } else if ("executed".equals(messageBase.getType())) {
            handleExecutedMessage(messageBase);
        } else if("execution_error".equals(messageBase.getType())) {
            handleExecutionErrorMessage(messageBase);
        } else if ("status".equals(messageBase.getType())) {
            handleStatusMessage(messageBase);
        }
    }
    
    // 处理进度消息：转发给前端
    private void handleProgressMessage(MessageBase messageBase) {
        HashMap<String, Object> data = messageBase.getData();
        String promptId = data.get("prompt_id").toString();
        ComfyuiTask task = redisService.getStartedTask(promptId);
        
        if (task != null) {
            data.put("type", "progress");
            wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(data));
        }
    }
    
    // 处理完成消息：保存结果并推送图片URL
    private void handleExecutedMessage(MessageBase messageBase) {
        // 提取图片信息并构造URL
        List<String> urls = images.stream()
            .map(image -> String.format("http://192.168.100.129:8188/view?filename=%s&type=%s", 
                image.get("filename"), image.get("type")))
            .collect(Collectors.toList());
        
        // 保存到数据库
        userResultService.saveList(urls, task.getUserId());
        
        // 推送给前端
        HashMap<String, Object> result = new HashMap<>();
        result.put("type", "imageResult");
        result.put("urls", urls);
        wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(result));
    }
    
    // 处理错误消息：归还积分并通知
    private void handleExecutionErrorMessage(MessageBase messageBase) {
        ComfyuiTask task = redisService.getStartedTask(promptId);
        
        // 归还冻结积分
        userFundRecordService.freezeReturn(task.getUserId(), task.getSize());
        
        // 推送错误消息
        wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(data));
    }
    
    // 处理状态消息：释放信号量
    private void handleStatusMessage(MessageBase messageBase) {
        Integer queueRemaining = (Integer) execInfo.get("queue_remaining");
        
        if (queueRemaining == 0) {
            // ComfyUI队列清空，释放信号量
            RSemaphore semaphore = redissonClient.getSemaphore(RunTaskJob.TASK_RUN_SEMAPHORE);
            semaphore.release();
        }
    }
}
```

---

## 🎨 完整的消息流转过程

### 场景：用户提交文生图任务

```text
1. 【前端】 用户点击"生成图片"按钮，建立WebSocket连接（携带clientId）
   ↓
2. 【后端服务端】 WebscoketUserInterceptor提取clientId并设置用户身份
   ↓
3. 【后端】 任务提交给ComfyUI执行（通过HTTP API）
   ↓
4. 【ComfyUI】 开始执行任务，推送消息到后端WebSocket客户端
   ↓
5. 【后端客户端】 ComfyuiMessageHandler接收消息
   ↓
6. 【后端业务层】 ComfyuiMessageService处理消息
   ↓
7. 【后端服务端】 WsNoticeService转发消息给前端（通过clientId定位）
   ↓
8. 【前端】 接收消息并更新UI（进度条/图片展示/错误提示）
```

---

## 📊 WebSocket在项目中的关键作用

### 1. 实时性保障
- ❌ **不使用WebSocket**：前端需要轮询查询任务状态（浪费资源、延迟高）
- ✅ **使用WebSocket**：任务状态变更立即推送（实时性强、用户体验好）

### 2. 双向通信设计
- **客户端角色**：监听ComfyUI任务进度，实现异步解耦
- **服务端角色**：向前端推送消息，实现实时反馈

### 3. 并发控制配合
- ComfyUI队列清空时，通过status消息触发信号量释放
- 定时任务根据信号量控制并发提交，防止GPU过载

### 4. 用户体验优化
- **进度条**：用户看到任务执行进度（5/20步）
- **即时展示**：图片生成完成立即显示
- **错误提示**：任务失败及时通知并归还积分

---

## 🔧 技术栈总结

### WebSocket服务端（向前端推送）
- **Spring WebSocket**：提供WebSocket支持
- **STOMP协议**：简化消息订阅和推送
- **SimpMessagingTemplate**：消息发送模板
- **ChannelInterceptor**：拦截器提取用户身份

### WebSocket客户端（连接ComfyUI）
- **StandardWebSocketClient**：基于JSR-356的WebSocket客户端
- **WebSocketConnectionManager**：管理连接生命周期
- **TextWebSocketHandler**：处理文本消息

### 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

---

## 📝 面试要点总结

### 问：为什么项目既做WebSocket服务端又做客户端？

**答：**
1. **作为服务端**：向前端推送实时消息（进度、结果、错误）
2. **作为客户端**：连接ComfyUI接收AI任务执行状态
3. **中间转发层**：将ComfyUI的消息处理后转发给前端对应用户

---

### 问：STOMP协议的作用是什么？

**答：**

 
- STOMP是基于WebSocket的高级消息协议
- 提供**订阅/发布**模式，简化消息路由
- 支持**点对点**（/user）和**广播**（/topic）两种模式
- 客户端使用简单，直接订阅主题即可接收消息

---

### 问：如何实现点对点消息推送？

**答：**

1. 客户端连接时在请求头携带`clientId`
2. `WebscoketUserInterceptor`提取`clientId`并设置为`Principal`
3. Spring将`clientId`与WebSocket会话绑定
4. 使用`convertAndSendToUser(clientId, destination, message)`推送


---

### 问：WebSocket断线重连如何处理？

**答：**
- **客户端**（连接ComfyUI）：Spring自动重连
- **服务端**（前端连接）：前端实现重连逻辑
- **任务恢复**：任务信息存储在Redis，重连后可继续推送

---

## ✅ 项目亮点

1. ✨ **双向WebSocket架构**：同时作为客户端和服务端，实现消息中转
2. ✨ **STOMP协议支持**：简化前端开发，支持灵活的订阅模式
3. ✨ **身份识别机制**：通过clientId实现精准的点对点推送
4. ✨ **消息类型丰富**：支持进度、结果、错误等多种消息类型
5. ✨ **业务逻辑解耦**：WebSocket层只负责通信，业务处理委托给Service层

---

**文档版本：** 1.0  
**最后更新：** 2024-10-26
