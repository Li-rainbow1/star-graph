package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;

import java.util.List;

/**
 * Ollama聊天请求类
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class OllamaChatRequest {

    /** 模型名称（如qwen2.5:0.5b） */
    private String model;
    
    /** 响应格式（json或null） */
    private String format;
    
    /** 是否启用流式响应 */
    private boolean stream;
    
    /** 模型保持活跃时间（如5m） */
    private String keepAlive="5m";
    
    /** 生成选项配置 */
    private OllamaChatOption options;
    
    /** 消息列表 */
    private List<OllamaMessage> messages;

}
