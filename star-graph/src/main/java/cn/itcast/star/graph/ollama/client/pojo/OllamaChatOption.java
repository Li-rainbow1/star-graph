package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;

import java.util.List;

/**
 * Ollama聊天选项配置类
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class OllamaChatOption {

    /** 随机种子 */
    private long seed;
    
    /** Top-K采样参数 */
    private int topK;
    
    /** Top-P采样参数 */
    private float topP;
    
    /** 重复惩罚回看窗口大小 */
    private int repeatLastN;
    
    /** 温度参数（0.0-2.0） */
    private float temperature;
    
    /** 重复惩罚系数 */
    private float repeatPenalty;
    
    /** 停止词列表 */
    private List<String> stop;
    
}
