package cn.itcast.star.graph.core.service;

/**
 * WebSocket消息推送服务 - 向前端推送实时消息（进度、结果、错误）
 */
public interface WsNoticeService {

    /**
     * 向指定客户端推送WebSocket消息
     */
    public void sendToUser(String clientId, String message);
}
