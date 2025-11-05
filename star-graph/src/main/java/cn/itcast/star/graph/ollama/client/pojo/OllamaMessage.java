package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;

/**
 * Ollama消息类
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class OllamaMessage {

    /** 消息角色（system/user/assistant） */
    private String role;
    
    /** 消息内容 */
    private String content;
    
    /** 图片数据 */
    private String images;

}
