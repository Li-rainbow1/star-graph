// 定义包路径
package cn.itcast.star.graph.core.wserver;

// 导入Spring消息抽象接口，代表一条消息
import org.springframework.messaging.Message;
// 导入Spring消息通道接口，用于消息的发送和接收
import org.springframework.messaging.MessageChannel;
// 导入STOMP命令枚举，用于识别STOMP协议的命令类型（如CONNECT、SUBSCRIBE等）
import org.springframework.messaging.simp.stomp.StompCommand;
// 导入STOMP消息头访问器，用于读取和修改STOMP消息的头部信息
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
// 导入通道拦截器接口，用于拦截消息发送前后的处理
import org.springframework.messaging.support.ChannelInterceptor;
// 导入消息头访问器工具类，用于从消息中提取访问器
import org.springframework.messaging.support.MessageHeaderAccessor;

// 导入Java安全包中的Principal接口，用于表示用户身份
import java.security.Principal;
// 导入List集合类
import java.util.List;

/**
 * WebSocket用户拦截器
 * 
 * <p>拦截WebSocket连接建立时的CONNECT消息，从请求头中提取clientId并设置为用户身份
 * 
 * <p>主要功能：
 * <ul>
 *     <li>拦截STOMP协议的CONNECT命令</li>
 *     <li>从消息头中提取clientId参数</li>
 *     <li>将clientId封装为Principal对象设置到会话中</li>
 *     <li>使后续可以通过clientId进行点对点消息推送</li>
 * </ul>
 * 
 * <p>工作流程：
 * <ol>
 *     <li>客户端建立WebSocket连接时，在请求头携带clientId</li>
 *     <li>拦截器提取clientId并设置为用户身份标识</li>
 *     <li>Spring WebSocket将clientId与WebSocket会话关联</li>
 *     <li>服务端可通过convertAndSendToUser(clientId, ...)发送消息</li>
 * </ol>
 * 
 * @author itcast
 * @since 1.0
 */
// 实现ChannelInterceptor接口，成为消息通道拦截器
public class WebscoketUserInterceptor implements ChannelInterceptor {

    /**
     * 消息发送前的拦截处理方法
     * 
     * <p>在消息发送到通道之前被调用，可以修改消息或阻止消息发送
     * 
     * @param message 要发送的消息对象
     * @param channel 消息通道
     * @return 处理后的消息对象，返回null则阻止消息发送
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 从消息中获取STOMP消息头访问器，用于读取和修改STOMP协议的头部信息
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        // 判断访问器不为空，且当前消息是CONNECT命令（客户端建立连接时发送）
        if (accessor != null && accessor.getCommand().equals(StompCommand.CONNECT)) {
            // 从STOMP消息头中获取客户端传递的"clientId"参数列表
            // getNativeHeader返回List是因为HTTP头可以有多个同名的值
            List<String> clientIds = accessor.getNativeHeader("clientId");
            
            // 判断clientId列表不为空且至少有一个元素
            if (clientIds != null && clientIds.size() > 0) {
                // 获取列表中的第一个clientId值（通常只有一个）
                String clientId = clientIds.get(0);
                
                // 创建一个匿名内部类实现Principal接口，将clientId封装为用户身份标识
                // 设置到STOMP会话中，Spring会将其与WebSocket会话关联
                accessor.setUser(new Principal() {
                    /**
                     * 返回用户身份的名称
                     * 
                     * @return 用户身份标识，这里返回clientId
                     */
                    @Override
                    public String getName() {
                        // 返回客户端ID作为用户身份名称
                        // 后续通过convertAndSendToUser(clientId, ...)即可向该用户推送消息
                        return clientId;
                    }
                });
            }
        }
        
        // 返回原始消息，允许消息继续发送
        // 如果返回null，则会阻止消息发送
        return message;
    }
}
