package cn.itcast.star.graph.comfyui.client.pojo;

import lombok.Data;

import java.util.UUID;

/**
 * ComfyUI任务对象
 * 
 * <p>表示一个提交给ComfyUI的文生图任务，包含任务的各项信息和状态
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

    /**
     * 无参构造函数（必需！供JSON反序列化使用）
     */
    public ComfyuiTask() {
        // 确保反序列化后ID不为null
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    /**
     * 构造ComfyUI任务
     * 
     * @param wsClientId WebSocket客户端ID
     * @param comfyuiRequestDto ComfyUI请求参数
     */
    public ComfyuiTask(String wsClientId, ComfyuiRequestDto comfyuiRequestDto) {
        this.wsClientId = wsClientId;
        this.comfyuiRequestDto = comfyuiRequestDto;
    }
}
