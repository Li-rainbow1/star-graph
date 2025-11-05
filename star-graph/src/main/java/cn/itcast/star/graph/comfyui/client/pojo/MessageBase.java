package cn.itcast.star.graph.comfyui.client.pojo;

import lombok.Data;

import java.util.HashMap;

/**
 * WebSocket消息基础类
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class MessageBase {
    /** 消息类型（status、progress、executing等） */
    private String type;
    
    /** 消息数据 */
    private HashMap<String, Object> data;
}
