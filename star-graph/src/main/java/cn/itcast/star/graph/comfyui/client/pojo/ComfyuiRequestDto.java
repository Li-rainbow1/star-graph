package cn.itcast.star.graph.comfyui.client.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * ComfyUI请求DTO
 * 
 * <p>向ComfyUI提交任务时的请求体结构
 * 
 * @author itcast
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
public class ComfyuiRequestDto {

    /**
     * 客户端ID，用于标识请求来源
     * <p>ComfyUI会根据此ID将执行结果通过WebSocket推送给对应客户端
     */
    @JsonProperty("client_id")
    String clientId;
    
    /**
     * 任务提示词配置
     * <p>实际是一个复杂的JSON对象，包含工作流的各个节点配置
     */
    Object prompt;

}
