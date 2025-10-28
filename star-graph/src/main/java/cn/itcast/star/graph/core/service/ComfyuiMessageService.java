package cn.itcast.star.graph.core.service;

import cn.itcast.star.graph.comfyui.client.pojo.MessageBase;

/**
 * ComfyUI消息处理服务接口
 * 
 * <p>负责处理从ComfyUI WebSocket接收到的各类消息，包括：
 * <ul>
 *     <li>progress - 任务执行进度消息（当前步数/总步数）</li>
 *     <li>executed - 任务执行完成消息（包含生成的图片信息）</li>
 *     <li>execution_error - 任务执行失败消息</li>
 *     <li>status - ComfyUI状态消息（队列剩余任务数）</li>
 * </ul>
 * 
 * <p>主要职责：
 * <ul>
 *     <li>解析不同类型的WebSocket消息</li>
 *     <li>处理任务完成后的图片保存和积分扣除</li>
 *     <li>处理任务失败后的积分归还</li>
 *     <li>管理信号量释放以控制并发任务数</li>
 *     <li>通过WebSocket向前端推送实时消息</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
public interface ComfyuiMessageService {
    /**
     * 处理ComfyUI发送的WebSocket消息
     * 
     * <p>根据消息类型分发到不同的处理方法：
     * <ul>
     *     <li>progress类型 - 推送实时进度给用户</li>
     *     <li>executed类型 - 保存图片、扣除积分、通知用户</li>
     *     <li>execution_error类型 - 归还积分、通知用户失败原因</li>
     *     <li>status类型 - 检查队列状态、释放信号量</li>
     * </ul>
     * 
     * @param messageBase ComfyUI消息对象，包含type和data字段
     */
    void handleMessage(MessageBase messageBase);
}
