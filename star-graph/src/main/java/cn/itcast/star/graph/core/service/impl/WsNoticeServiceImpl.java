package cn.itcast.star.graph.core.service.impl;

import cn.itcast.star.graph.core.service.WsNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket消息推送服务实现
 */
@Service
public class WsNoticeServiceImpl implements WsNoticeService {
    // WebSocket消息主题路径
    public final static String COMFYUI_QUEUE_TOPIC = "/topic/messages";

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 向指定客户端推送消息（点对点）
     */
    @Override
    public void sendToUser(String clientId, String message) {
        simpMessagingTemplate.convertAndSendToUser(clientId, COMFYUI_QUEUE_TOPIC, message);
    }
}
