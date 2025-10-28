package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;

/**
 * Ollama统计信息类
 * 
 * <p>记录模型推理过程的性能统计数据
 * 
 * <p>作为OllamaChatRespone的父类，提供以下统计信息：
 * <ul>
 *     <li>任务完成状态</li>
 *     <li>各阶段耗时（加载模型、处理输入、生成输出）</li>
 *     <li>处理的token数量</li>
 * </ul>
 * 
 * <p>这些统计数据可用于：
 * <ul>
 *     <li>性能分析和优化</li>
 *     <li>成本计算（根据token数量）</li>
 *     <li>监控系统负载</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
@Data  // Lombok注解，自动生成getter/setter/toString/equals/hashCode方法
public class OllamaStatisitic {

    /**
     * 任务是否完成
     * 
     * <p>true：模型已完成生成，响应完整
     * <p>false：正在生成中（流式响应时会出现）
     * 
     * <p>非流式请求通常始终为true
     */
    private boolean done;
    
    /**
     * 总耗时（纳秒）
     * 
     * <p>从接收请求到返回响应的总时间
     * <p>单位：纳秒（1秒=1,000,000,000纳秒）
     * <p>包含：模型加载 + 输入处理 + 生成输出
     * 
     * <p>转换为秒：totalDuration / 1_000_000_000.0
     */
    private long totalDuration;
    
    /**
     * 模型加载耗时（纳秒）
     * 
     * <p>将模型从磁盘加载到内存/显存的时间
     * <p>如果模型已在内存中，此值为0
     * <p>首次调用或长时间未使用后会重新加载
     */
    private long loadDuration;
    
    /**
     * 输入提示词评估的token数量
     * 
     * <p>模型处理的输入token数（包含系统提示和用户输入）
     * <p>token是模型处理文本的基本单位，中文通常1-2个字符为1个token
     * <p>示例：输入"你好世界"可能包含4-6个token
     */
    private long promptEvalCount;
    
    /**
     * 输入提示词评估耗时（纳秒）
     * 
     * <p>模型处理输入（编码、理解）的时间
     * <p>包含对输入的分词、嵌入、注意力计算等
     */
    private long promptEvalDuration;
    
    /**
     * 生成输出的token数量
     * 
     * <p>模型生成的输出token数
     * <p>示例：生成"Hello world"可能包含3-4个token
     * 
     * <p>生成速度 = evalCount / (evalDuration / 1_000_000_000.0) token/秒
     */
    private long evalCount;
    
    /**
     * 生成输出耗时（纳秒）
     * 
     * <p>模型生成输出文本的时间
     * <p>通常是整个过程中最耗时的阶段
     * <p>取决于生成的token数量和模型大小
     */
    private long evalDuration;
}
