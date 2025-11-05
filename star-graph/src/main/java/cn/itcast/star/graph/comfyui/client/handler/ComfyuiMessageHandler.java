package cn.itcast.star.graph.comfyui.client.handler;

import cn.itcast.star.graph.comfyui.client.pojo.MessageBase;
import cn.itcast.star.graph.core.service.ComfyuiMessageService;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * ComfyUI WebSocket消息处理器 - 接收ComfyUI推送的任务进度和结果消息
 */
@Slf4j
@Component
public class ComfyuiMessageHandler extends TextWebSocketHandler {
    
    @Autowired
    ComfyuiMessageService comfyuiMessageService;

    /**
     * WebSocket连接成功回调
     */
    @Override  
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("ComfyUI WebSocket连接成功");
    }

    /**
     * 接收并处理ComfyUI的WebSocket消息
     */
    @Override  
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        MessageBase messageBase = JSON.parseObject(payload, MessageBase.class);
        comfyuiMessageService.handleMessage(messageBase);
        log.debug("收到ComfyUI消息: {}", payload);
    }
}
