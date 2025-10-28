package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;

/**
 * Ollama聊天响应类
 * 
 * <p>封装Ollama API返回的聊天响应数据
 * 
 * <p>继承自OllamaStatisitic，包含统计信息（耗时、token数等）
 * 
 * <p>响应结构：
 * <pre>
 * {
 *   "model": "qwen2.5:0.5b",
 *   "created_at": "2024-10-25T13:30:00.123456Z",
 *   "message": {
 *     "role": "assistant",
 *     "content": "翻译结果..."
 *   },
 *   "done": true,
 *   "total_duration": 1234567890,
 *   ...其他统计信息
 * }
 * </pre>
 * 
 * @author itcast
 * @since 1.0
 * @see OllamaStatisitic 统计信息基类
 */
@Data  // Lombok注解，自动生成getter/setter/toString/equals/hashCode方法
// 继承OllamaStatisitic类，复用done、totalDuration等统计字段
public class OllamaChatRespone extends OllamaStatisitic {

    /**
     * 使用的模型名称
     * 
     * <p>返回实际执行推理的模型
     * <p>通常与请求中的model参数一致
     * <p>示例：qwen2.5:0.5b
     */
    private String model;
    
    /**
     * 创建时间
     * 
     * <p>响应生成的时间戳（UTC时区）
     * <p>格式：ISO 8601标准（YYYY-MM-DDTHH:mm:ss.ffffffZ）
     * <p>示例：2024-10-25T13:30:00.123456Z
     */
    private String created_at;
    
    /**
     * 模型生成的消息
     * 
     * <p>包含角色（assistant）和内容（生成的文本）
     * <p>这是响应中最重要的字段，包含了模型的实际输出
     * <p>通过message.getContent()即可获取翻译结果
     * 
     * @see OllamaMessage
     */
    private OllamaMessage message;

}
