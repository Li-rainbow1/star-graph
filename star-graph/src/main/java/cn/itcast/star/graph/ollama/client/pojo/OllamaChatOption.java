package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;

import java.util.List;

/**
 * Ollama聊天选项配置类
 * 
 * <p>用于配置Ollama大语言模型的生成参数，控制模型的输出行为和质量
 * 
 * <p>这些参数影响模型生成的多样性、随机性、重复性等特征，
 * 适当调整可以获得更符合预期的输出结果
 * 
 * @author itcast
 * @since 1.0
 */
@Data  // Lombok注解，自动生成getter/setter/toString/equals/hashCode方法
public class OllamaChatOption {

    /**
     * 随机种子
     * 
     * <p>用于控制模型输出的随机性，相同的seed会产生相同的输出
     * <p>用途：保证可重复性，便于测试和调试
     */
    private long seed;
    
    /**
     * Top-K采样参数
     * 
     * <p>从概率最高的K个token中随机选择下一个token
     * <p>较小的值（如10-40）会让输出更加确定，较大的值会增加多样性
     * <p>默认值通常为40
     */
    private int topK;
    
    /**
     * Top-P采样参数（核采样）
     * 
     * <p>从累积概率达到P的token集合中随机选择
     * <p>取值范围：0.0-1.0，常用值0.9-0.95
     * <p>较小的值会让输出更加确定，接近1.0会增加多样性
     */
    private float topP;
    
    /**
     * 重复惩罚回看窗口大小
     * 
     * <p>在计算重复惩罚时，回看最近N个token
     * <p>用于防止模型生成重复内容
     * <p>默认值通常为64
     */
    private int repeatLastN;
    
    /**
     * 温度参数
     * 
     * <p>控制输出的随机性和创造性
     * <p>取值范围：0.0-2.0
     * <p>0.0：完全确定（选择概率最高的token）
     * <p>1.0：标准随机性
     * <p>>1.0：增加随机性和创造性
     * <p>翻译任务通常使用较低温度（0.3-0.7）
     */
    private float temperature;
    
    /**
     * 重复惩罚系数
     * 
     * <p>对已经出现过的token施加惩罚，降低其被再次选中的概率
     * <p>取值范围：通常1.0-1.5
     * <p>1.0：不惩罚
     * <p>>1.0：惩罚力度增加，越大越能避免重复
     */
    private float repeatPenalty;
    
    /**
     * 停止词列表
     * 
     * <p>当模型生成这些词时立即停止生成
     * <p>用于控制输出长度和格式
     * <p>示例：["</s>", "\n\n", "###"]
     */
    private List<String> stop;
    
}
