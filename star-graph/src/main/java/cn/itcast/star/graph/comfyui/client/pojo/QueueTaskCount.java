package cn.itcast.star.graph.comfyui.client.pojo;

import lombok.Builder;
import lombok.Data;

/**
 * 队列任务数量
 * 
 * <p>查询ComfyUI队列中任务数量的响应结构
 * 
 * @author itcast
 * @since 1.0
 */
@Data
@Builder
public class QueueTaskCount {
    /**
     * 执行信息，包含队列剩余任务数
     */
    ExecInfo execInfo;
}
