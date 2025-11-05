package cn.itcast.star.graph.core.wserver;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.List;

/**
 * WebSocket用户拦截器 - 从CONNECT消息提取clientId作为用户标识
 */
public class WebscoketUserInterceptor implements ChannelInterceptor {

    /**
     * 拦截CONNECT消息，提取clientId并设置为用户身份
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && accessor.getCommand().equals(StompCommand.CONNECT)) {
            List<String> clientIds = accessor.getNativeHeader("clientId");
            
            if (clientIds != null && clientIds.size() > 0) {
                String clientId = clientIds.get(0);
                
                // 将clientId设置为用户身份，用于点对点消息推送
                accessor.setUser(new Principal() {
                    @Override
                    public String getName() {
                        return clientId;
                    }
                });
            }
        }
        
        return message;
    }
}
