package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;

/**
 * Ollama统计信息类
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class OllamaStatisitic {

    /** 任务是否完成 */
    private boolean done;
    
    /** 总耗时（纳秒） */
    private long totalDuration;
    
    /** 模型加载耗时（纳秒） */
    private long loadDuration;
    
    /** 输入提示词的token数量 */
    private long promptEvalCount;
    
    /** 输入提示词评估耗时（纳秒） */
    private long promptEvalDuration;
    
    /** 生成输出的token数量 */
    private long evalCount;
    
    /** 生成输出耗时（纳秒） */
    private long evalDuration;
}
