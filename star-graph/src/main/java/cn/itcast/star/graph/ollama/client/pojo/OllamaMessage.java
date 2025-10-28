package cn.itcast.star.graph.ollama.client.pojo;

import lombok.Data;

/**
 * Ollama消息类
 * 
 * <p>表示对话中的一条消息，包含角色和内容
 * 
 * <p>在请求和响应中都会使用此类：
 * <ul>
 *     <li>请求中：构造用户输入和系统提示</li>
 *     <li>响应中：获取模型生成的回复</li>
 * </ul>
 * 
 * <p>消息格式示例：
 * <pre>
 * // 系统提示
 * {
 *   "role": "system",
 *   "content": "你是一个专业的翻译助手"
 * }
 * 
 * // 用户输入
 * {
 *   "role": "user",
 *   "content": "请将'美丽的风景'翻译成英文"
 * }
 * 
 * // 模型回复
 * {
 *   "role": "assistant",
 *   "content": "Beautiful scenery"
 * }
 * </pre>
 * 
 * @author itcast
 * @since 1.0
 */
@Data  // Lombok注解，自动生成getter/setter/toString/equals/hashCode方法
public class OllamaMessage {

    /**
     * 消息角色
     * 
     * <p>标识消息的发送者类型
     * <p>可选值：
     * <ul>
     *     <li>system - 系统消息，用于设定模型行为和约束</li>
     *     <li>user - 用户消息，表示人类的输入</li>
     *     <li>assistant - 助手消息，表示模型的回复</li>
     * </ul>
     * 
     * <p>本项目使用场景：
     * <ul>
     *     <li>发送翻译请求时：role="user"</li>
     *     <li>接收翻译结果时：role="assistant"</li>
     * </ul>
     */
    private String role;
    
    /**
     * 消息内容
     * 
     * <p>消息的实际文本内容
     * 
     * <p>根据角色不同，内容含义不同：
     * <ul>
     *     <li>system角色：系统提示词，如"你是一个翻译助手"</li>
     *     <li>user角色：用户输入，如"帮我把'你好'翻译成英文"</li>
     *     <li>assistant角色：模型生成的回复，如"Hello"</li>
     * </ul>
     */
    private String content;
    
    /**
     * 图片数据（Base64编码或URL）
     * 
     * <p>用于多模态模型（支持图文理解的模型）
     * <p>可以是：
     * <ul>
     *     <li>Base64编码的图片数据</li>
     *     <li>图片的URL地址</li>
     * </ul>
     * 
     * <p>本项目暂未使用此功能，用于文本翻译不需要图片
     */
    private String images;

}
