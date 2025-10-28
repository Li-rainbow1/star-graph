package cn.itcast.star.graph.comfyui.client.pojo;

import lombok.Data;

import java.util.HashMap;

/**
 * WebSocket消息基础类
 * 
 * <p>从ComfyUI接收到的WebSocket消息的通用结构，所有消息都包含类型和数据两部分
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class MessageBase {
    /**
     * 消息类型，如：status、progress、executing等
     * <p>不同类型的消息需要不同的处理逻辑
     */
    private String type;
    
    /**
     * 消息数据，存储消息的具体内容
     * <p>数据结构根据消息类型不同而不同
     */
    private HashMap<String, Object> data;
}
