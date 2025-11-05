package cn.itcast.star.graph.comfyui.client.pojo;

import lombok.Data;

import java.util.UUID;

/**
 * ComfyUI任务对象
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class ComfyuiTask {

    /** 本地任务ID，系统内部使用 */
    private String id = UUID.randomUUID().toString();
    
    /** WebSocket客户端ID */
    String wsClientId;
    
    /** ComfyUI请求参数 */
    ComfyuiRequestDto comfyuiRequestDto;
    
    /** ComfyUI中任务的唯一ID，提交后由ComfyUI返回 */
    String promptId;
    
    /** 用户ID，标识任务所属用户 */
    Long userId;
    
    /** 当前任务在队列中的序号位置 */
    long index;
    
    /** 生成的图片数量 */
    int size;

    public ComfyuiTask() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    public ComfyuiTask(String wsClientId, ComfyuiRequestDto comfyuiRequestDto) {
        this.wsClientId = wsClientId;
        this.comfyuiRequestDto = comfyuiRequestDto;
    }
}
