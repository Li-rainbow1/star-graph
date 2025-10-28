// 定义包路径
package cn.itcast.star.graph.core.config;

// 导入自定义的WebSocket用户拦截器，用于从连接请求中提取clientId
import cn.itcast.star.graph.core.wserver.WebscoketUserInterceptor;
// 导入Spring的Configuration注解，标识这是一个配置类
import org.springframework.context.annotation.Configuration;
// 导入Spring消息通道注册器，用于配置客户端入站通道
import org.springframework.messaging.simp.config.ChannelRegistration;
// 导入Spring消息代理注册器，用于配置消息代理
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
// 导入Spring的WebSocket消息代理启用注解
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
// 导入STOMP端点注册器，用于注册WebSocket连接端点
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
// 导入WebSocket消息代理配置器接口
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
// 导入WebSocket传输注册器（虽然未使用，但保留导入）
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
// 导入HTTP会话握手拦截器，用于在WebSocket握手时共享HTTP Session
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * WebSocket配置类
 * 
 * <p>配置基于STOMP协议的WebSocket消息代理，实现服务端向客户端的实时消息推送
 * 
 * <p>主要功能：
 * <ul>
 *     <li>配置WebSocket连接端点（/ws）</li>
 *     <li>配置消息代理（/topic广播、/user点对点）</li>
 *     <li>配置用户身份识别（通过clientId）</li>
 *     <li>支持跨域连接</li>
 * </ul>
 * 
 * <p>应用场景：
 * <ul>
 *     <li>任务队列变化通知（实时显示排队位置）</li>
 *     <li>任务执行进度更新（显示进度条）</li>
 *     <li>图片生成完成通知（推送生成结果）</li>
 *     <li>任务失败通知（错误提示）</li>
 * </ul>
 * 
 * <p>STOMP协议说明：
 * <ul>
 *     <li>STOMP（Simple Text Oriented Messaging Protocol）是一个简单的面向文本的消息协议</li>
 *     <li>在WebSocket之上提供了更高级的消息传递语义</li>
 *     <li>支持发布/订阅模式，简化了WebSocket的使用</li>
 *     <li>客户端可以使用STOMP.js库轻松连接</li>
 * </ul>
 * 
 * <p>连接流程：
 * <ol>
 *     <li>客户端连接到/ws端点，携带clientId</li>
 *     <li>WebSocket握手成功，建立持久连接</li>
 *     <li>客户端订阅主题（如/user/{clientId}/topic/messages）</li>
 *     <li>服务端通过SimpMessagingTemplate推送消息</li>
 *     <li>客户端实时接收并处理消息</li>
 * </ol>
 * 
 * @author itcast
 * @since 1.0
 */
// 标识这是一个Spring配置类
@Configuration
// 启用WebSocket消息代理功能，支持STOMP协议
@EnableWebSocketMessageBroker
// 实现WebSocketMessageBrokerConfigurer接口，自定义WebSocket配置
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    /**
     * 注册STOMP端点配置方法
     * 
     * <p>配置WebSocket的连接端点，客户端通过此端点建立WebSocket连接
     * 
     * <p>端点配置说明：
     * <ul>
     *     <li>端点路径：/ws（客户端连接地址：ws://host:port/ws）</li>
     *     <li>握手拦截器：HttpSessionHandshakeInterceptor（共享HTTP Session）</li>
     *     <li>跨域配置：允许所有域名访问（*）</li>
     * </ul>
     * 
     * <p>客户端连接示例（JavaScript）：
     * <pre>
     * const socket = new SockJS('http://localhost:8080/ws');
     * const stompClient = Stomp.over(socket);
     * stompClient.connect({clientId: 'xxx'}, function(frame) {
     *     console.log('Connected: ' + frame);
     *     stompClient.subscribe('/user/queue/messages', function(message) {
     *         console.log(message.body);
     *     });
     * });
     * </pre>
     * 
     * @param registry STOMP端点注册器，用于注册和配置WebSocket端点
     */
    @Override  // 重写父接口的方法
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket连接端点
        // addEndpoint("/ws")：添加一个端点，路径为/ws
        // 客户端将通过ws://host:port/ws或http://host:port/ws（使用SockJS时）连接
        registry.addEndpoint("/ws")
                // 添加HTTP会话握手拦截器
                // 作用：在WebSocket握手时，将HTTP Session中的属性复制到WebSocket Session
                // 这样可以在WebSocket连接中访问HTTP Session中的数据（如用户信息）
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                // 设置允许跨域的源模式
                // "*"：允许所有域名访问，适合开发环境
                // 生产环境建议明确指定允许的域名，如：setAllowedOrigins("https://example.com")
                .setAllowedOriginPatterns("*");
    }

    /**
     * 配置消息代理（Message Broker）
     * 
     * <p>定义消息的路由规则，指定哪些前缀的消息由简单消息代理处理
     * 
     * <p>消息代理说明：
     * <ul>
     *     <li>/topic：用于广播消息，所有订阅该主题的客户端都能收到</li>
     *     <li>/user：用于点对点消息，只有指定的用户能收到</li>
     * </ul>
     * 
     * <p>使用示例：
     * <ul>
     *     <li>广播消息：simpMessagingTemplate.convertAndSend("/topic/messages", data)</li>
     *     <li>点对点消息：simpMessagingTemplate.convertAndSendToUser(clientId, "/topic/messages", data)</li>
     * </ul>
     * 
     * <p>客户端订阅示例：
     * <ul>
     *     <li>订阅广播：stompClient.subscribe('/topic/messages', callback)</li>
     *     <li>订阅个人消息：stompClient.subscribe('/user/queue/messages', callback)</li>
     * </ul>
     * 
     * <p>路径转换：
     * <ul>
     *     <li>服务端发送：/user/{clientId}/topic/messages</li>
     *     <li>客户端接收：/user/topic/messages（Spring自动处理）</li>
     * </ul>
     * 
     * @param registry 消息代理注册器，用于配置消息路由规则
     */
    @Override  // 重写父接口的方法
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理（Simple Broker）
        // enableSimpleBroker()：启用内存中的简单消息代理（适合单机应用）
        // 如果是集群环境，建议使用外部消息代理（如RabbitMQ、ActiveMQ）
        // 参数：指定代理处理的目的地前缀
        // "/topic"：用于广播消息，一对多通信模式
        //           服务端发送到/topic的消息会被推送给所有订阅该主题的客户端
        // "/user"：用于点对点消息，一对一通信模式
        //          服务端通过convertAndSendToUser发送的消息只会推送给指定的用户
        registry.enableSimpleBroker("/topic", "/user");
        
        // 【可选配置】设置应用程序目的地前缀（客户端发送消息到服务端时使用）
        // registry.setApplicationDestinationPrefixes("/app");
        // 客户端发送：stompClient.send("/app/hello", {}, data)
        // 服务端接收：@MessageMapping("/hello")
    }

    /**
     * 配置客户端入站通道（Client Inbound Channel）
     * 
     * <p>在客户端消息进入服务端之前进行拦截处理，用于提取和设置用户身份信息
     * 
     * <p>拦截器作用：
     * <ul>
     *     <li>从CONNECT消息的请求头中提取clientId</li>
     *     <li>将clientId封装为Principal并设置到WebSocket会话中</li>
     *     <li>使服务端可以通过clientId向指定用户推送消息</li>
     * </ul>
     * 
     * <p>工作流程：
     * <ol>
     *     <li>客户端连接时在请求头携带clientId</li>
     *     <li>WebscoketUserInterceptor拦截CONNECT消息</li>
     *     <li>提取clientId并设置为用户身份标识</li>
     *     <li>后续可通过convertAndSendToUser(clientId, ...)发送消息</li>
     * </ol>
     * 
     * <p>客户端连接示例（携带clientId）：
     * <pre>
     * const headers = {
     *     clientId: 'user-123'
     * };
     * stompClient.connect(headers, onConnected, onError);
     * </pre>
     * 
     * @param registration 客户端入站通道注册器，用于配置拦截器
     * @see WebscoketUserInterceptor 用户身份拦截器实现
     */
    @Override  // 重写父接口的方法
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 向客户端入站通道添加拦截器
        // registration.interceptors()：添加一个或多个拦截器到拦截器链
        // new WebscoketUserInterceptor()：创建用户身份拦截器实例
        // 该拦截器会在客户端消息到达服务端之前执行
        // 主要功能：从STOMP的CONNECT命令中提取clientId，并设置为WebSocket会话的用户身份
        // 这样服务端就可以通过convertAndSendToUser(clientId, destination, payload)
        // 向指定的clientId用户发送点对点消息
        registration.interceptors(new WebscoketUserInterceptor());
    }
}
