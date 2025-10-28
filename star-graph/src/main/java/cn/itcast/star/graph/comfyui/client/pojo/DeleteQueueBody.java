package cn.itcast.star.graph.comfyui.client.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 删除队列请求体
 * 
 * <p>用于向ComfyUI请求删除队列中的任务
 * 
 * @author itcast
 * @since 1.0
 */
@Data
@Builder
public class DeleteQueueBody {
    /**
     * 要删除的任务ID列表
     * <p>对应ComfyUI中的promptId
     */
    List<String> delete;
}
