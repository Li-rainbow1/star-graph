package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Ollama聊天响应类
 * 
 * @author itcast
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OllamaChatRespone extends OllamaStatisitic {

    /** 使用的模型名称 */
    private String model;
    
    /** 创建时间 */
    private String created_at;
    
    /** 模型生成的消息 */
    private OllamaMessage message;

}
