package cn.itcast.star.graph.core.service.impl;

import cn.itcast.star.graph.core.service.WsNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket消息推送服务实现
 * 
 * <p>使用Spring WebSocket的STOMP协议向前端推送消息
 * 
 * @author itcast
 * @since 1.0
 */
@Service
public class WsNoticeServiceImpl implements WsNoticeService {
    /** WebSocket消息主题路径 */
    public final static String COMFYUI_QUEUE_TOPIC = "/topic/messages";

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void sendToUser(String clientId, String message) {
        // 向指定用户推送消息（点对点消息）
        // 参数1：客户端ID（唯一标识一个WebSocket连接）
        // 参数2：消息主题路径
        // 参数3：消息内容（JSON字符串）
        simpMessagingTemplate.convertAndSendToUser(clientId, COMFYUI_QUEUE_TOPIC, message);
    }
}
