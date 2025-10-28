package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;

import java.util.List;

/**
 * Ollama聊天请求类
 * 
 * <p>封装向Ollama API发送聊天请求的所有参数
 * 
 * <p>请求流程：
 * <ol>
 *     <li>指定要使用的模型（如qwen2.5:0.5b）</li>
 *     <li>构造消息列表（包含用户输入和历史对话）</li>
 *     <li>配置生成选项（温度、采样参数等）</li>
 *     <li>发送到Ollama API的/api/chat端点</li>
 * </ol>
 * 
 * @author itcast
 * @since 1.0
 */
@Data  // Lombok注解，自动生成getter/setter/toString/equals/hashCode方法
public class OllamaChatRequest {

    /**
     * 模型名称
     * 
     * <p>指定要使用的Ollama模型，必须是已下载的模型
     * <p>示例：
     * <ul>
     *     <li>qwen2.5:0.5b - 通义千问2.5（0.5B参数版本，轻量快速）</li>
     *     <li>qwen2.5:7b - 通义千问2.5（7B参数版本，效果更好）</li>
     *     <li>llama3 - Meta的Llama 3模型</li>
     *     <li>mistral - Mistral AI的模型</li>
     * </ul>
     */
    private String model;
    
    /**
     * 响应格式
     * 
     * <p>指定API返回的数据格式
     * <p>可选值：
     * <ul>
     *     <li>json - 返回JSON格式（默认）</li>
     *     <li>null - 返回纯文本</li>
     * </ul>
     */
    private String format;
    
    /**
     * 是否启用流式响应
     * 
     * <p>true：以SSE（Server-Sent Events）方式逐字返回，适合实时显示
     * <p>false：等待全部生成完毕后一次性返回，适合批量处理
     * <p>本项目使用false，一次性获取完整翻译结果
     */
    private boolean stream;
    
    /**
     * 模型保持活跃时间
     * 
     * <p>模型加载到内存后的保留时间，超时后自动卸载释放内存
     * <p>格式：数字+单位（s秒/m分钟/h小时）
     * <p>示例：
     * <ul>
     *     <li>5m - 5分钟（默认）</li>
     *     <li>30s - 30秒</li>
     *     <li>1h - 1小时</li>
     * </ul>
     */
    private String keepAlive="5m";
    
    /**
     * 生成选项配置
     * 
     * <p>控制模型生成行为的参数，如温度、采样方式、重复惩罚等
     * <p>可选，不设置则使用模型的默认参数
     * 
     * @see OllamaChatOption
     */
    private OllamaChatOption options;
    
    /**
     * 消息列表
     * 
     * <p>包含对话历史和当前用户输入
     * <p>格式：[{role: "system", content: "..."}, {role: "user", content: "..."}]
     * <p>角色类型：
     * <ul>
     *     <li>system - 系统提示（设定模型行为）</li>
     *     <li>user - 用户输入</li>
     *     <li>assistant - 模型回复（用于多轮对话）</li>
     * </ul>
     * 
     * @see OllamaMessage
     */
    private List<OllamaMessage> messages;

}
