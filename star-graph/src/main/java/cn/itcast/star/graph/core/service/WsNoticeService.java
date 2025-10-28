package cn.itcast.star.graph.core.service;

/**
 * WebSocket消息推送服务接口
 * 
 * <p>基于Spring WebSocket和STOMP协议向前端推送实时消息
 * 
 * <p>主要用途：
 * <ul>
 *     <li>推送任务执行进度（progress消息）</li>
 *     <li>推送图片生成结果（imageResult消息）</li>
 *     <li>推送任务执行失败信息（execution_error消息）</li>
 * </ul>
 * 
 * <p>技术实现：
 * <ul>
 *     <li>使用Spring的SimpMessagingTemplate进行消息推送</li>
 *     <li>通过clientId实现点对点精准推送</li>
 *     <li>消息主题路径：/topic/messages</li>
 *     <li>前端通过STOMP订阅该主题接收消息</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
public interface WsNoticeService {

    /**
     * 向指定用户发送WebSocket消息（点对点）
     * 
     * <p>使用convertAndSendToUser方法，只有指定clientId的WebSocket连接能收到消息
     * 
     * <p>使用场景：
     * <ul>
     *     <li>推送任务进度给任务所有者</li>
     *     <li>推送图片生成结果给任务所有者</li>
     *     <li>推送任务失败信息给任务所有者</li>
     * </ul>
     * 
     * @param clientId WebSocket客户端ID，唯一标识一个WebSocket连接
     * @param message 消息内容，通常是JSON字符串
     */
    public void sendToUser(String clientId, String message);
}
