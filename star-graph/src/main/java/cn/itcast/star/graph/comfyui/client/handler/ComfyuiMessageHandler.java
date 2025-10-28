// 定义包路径
package cn.itcast.star.graph.comfyui.client.handler;

// 导入ComfyUI消息基类，封装WebSocket消息的通用结构
import cn.itcast.star.graph.comfyui.client.pojo.MessageBase;
// 导入ComfyUI消息处理服务接口，负责业务逻辑处理
import cn.itcast.star.graph.core.service.ComfyuiMessageService;
// 导入FastJSON工具类，用于JSON字符串和对象之间的转换
import com.alibaba.fastjson2.JSON;
// 导入Spring的Autowired注解，用于依赖注入
import org.springframework.beans.factory.annotation.Autowired;
// 导入Spring的Component注解，将此类注册为Spring容器管理的Bean
import org.springframework.stereotype.Component;
// 导入Spring WebSocket的文本消息类，封装接收到的文本消息
import org.springframework.web.socket.TextMessage;
// 导入Spring WebSocket会话类，代表一个WebSocket连接
import org.springframework.web.socket.WebSocketSession;
// 导入Spring WebSocket的文本消息处理器抽象类
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * ComfyUI WebSocket消息处理器
 * 
 * <p>作为WebSocket客户端，接收并处理从ComfyUI服务端推送过来的消息
 * 
 * <p>主要消息类型：
 * <ul>
 *     <li>status消息：ComfyUI队列状态变更（任务开始执行、队列为空等）</li>
 *     <li>progress消息：任务执行进度更新（当前步数/总步数）</li>
 *     <li>executed消息：节点执行完成，包含生成的图片信息</li>
 *     <li>execution_error消息：任务执行失败的错误信息</li>
 * </ul>
 * 
 * <p>工作流程：
 * <ol>
 *     <li>应用启动时，通过ComfyuiConfig建立到ComfyUI的WebSocket连接</li>
 *     <li>ComfyUI在任务状态变化时推送消息到此处理器</li>
 *     <li>处理器解析JSON消息为MessageBase对象</li>
 *     <li>委托给ComfyuiMessageService进行业务处理</li>
 *     <li>业务处理包括：更新数据库、释放信号量、推送通知给前端等</li>
 * </ol>
 * 
 * <p>设计模式：
 * <ul>
 *     <li>Handler模式：此类负责接收和初步解析消息</li>
 *     <li>委托模式：具体业务逻辑委托给ComfyuiMessageService</li>
 *     <li>职责单一：只负责WebSocket层面的消息接收，不涉及业务逻辑</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 * @see ComfyuiMessageService 消息业务处理服务
 * @see MessageBase 消息基类
 */
// 标识这是一个Spring组件，会被自动扫描并注册到容器
@Component
// 继承TextWebSocketHandler抽象类，实现文本消息的WebSocket处理
// TextWebSocketHandler提供了WebSocket生命周期的各种回调方法
public class ComfyuiMessageHandler extends TextWebSocketHandler {
    
    // 注入ComfyUI消息处理服务，用于处理接收到的消息
    @Autowired
    ComfyuiMessageService comfyuiMessageService;


    /**
     * WebSocket连接建立成功后的回调方法
     * 
     * <p>当WebSocket客户端成功连接到ComfyUI服务端时，此方法会被自动调用
     * 
     * <p>调用时机：
     * <ul>
     *     <li>应用启动时，ComfyuiConfig建立WebSocket连接后</li>
     *     <li>断线重连成功后</li>
     * </ul>
     * 
     * <p>可以在此方法中：
     * <ul>
     *     <li>记录连接成功日志</li>
     *     <li>初始化连接相关的资源</li>
     *     <li>发送初始化消息给服务端</li>
     * </ul>
     * 
     * @param session WebSocket会话对象，代表此次连接
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override  // 重写父类的方法
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 打印连接成功的日志到控制台
        // 生产环境建议使用日志框架（如slf4j）记录日志
        System.out.println("=============连接成功");
        
        // 【可扩展】可以在这里添加更多逻辑：
        // 例如：记录连接时间、初始化心跳检测、发送认证消息等
    }

    /**
     * 接收并处理WebSocket文本消息的核心方法
     * 
     * <p>当从ComfyUI服务端接收到文本消息时，此方法会被自动调用
     * 
     * <p>消息处理流程：
     * <ol>
     *     <li>提取消息载荷（JSON字符串）</li>
     *     <li>将JSON解析为MessageBase对象</li>
     *     <li>委托给ComfyuiMessageService进行业务处理</li>
     *     <li>打印接收日志</li>
     * </ol>
     * 
     * <p>消息格式示例：
     * <pre>
     * {
     *   "type": "progress",
     *   "data": {
     *     "value": 5,
     *     "max": 20,
     *     "prompt_id": "xxx"
     *   }
     * }
     * </pre>
     * 
     * @param session WebSocket会话对象，代表此次连接
     * @param message 接收到的文本消息对象，包含消息内容
     * @throws Exception 处理过程中可能抛出的异常（如JSON解析失败）
     */
    @Override  // 重写父类的方法
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 从TextMessage对象中提取消息载荷（payload），即实际的JSON字符串内容
        // getPayload()返回消息的实际内容（纯文本）
        String payload = message.getPayload();
        
        // 使用FastJSON将JSON字符串解析为MessageBase对象
        // parseObject()：将JSON字符串转换为指定类型的Java对象
        // MessageBase.class：目标对象类型，包含type和data两个字段
        MessageBase messageBase = JSON.parseObject(payload, MessageBase.class);
        
        // 委托给ComfyuiMessageService处理消息
        // handleMessage()会根据消息类型（type）分发到不同的处理方法
        // 例如：progress消息 -> handleProgressMessage()
        //      executed消息 -> handleExecutedMessage()
        comfyuiMessageService.handleMessage(messageBase);
        
        // 打印接收到的消息到控制台，便于调试和监控
        // 将message对象转换为JSON字符串输出
        // 生产环境建议使用日志框架（如slf4j）并设置合适的日志级别
        System.out.println("=============收到消息:"+ JSON.toJSONString(message));
    }
}
