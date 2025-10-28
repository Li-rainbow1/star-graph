package cn.itcast.star.graph.core.service;

/**
 * Ollama AI翻译服务接口
 * 
 * <p>使用Ollama本地大语言模型进行中英文翻译
 * 
 * <p>主要用途：
 * <ul>
 *     <li>将用户输入的中文提示词翻译为英文</li>
 *     <li>将用户输入的中文负向提示词翻译为英文</li>
 *     <li>提高ComfyUI生成图片的质量（英文提示词效果更好）</li>
 * </ul>
 * 
 * <p>技术实现：
 * <ul>
 *     <li>使用Ollama本地部署的qwen2.5:0.5b模型（通义千问）</li>
 *     <li>0.5B参数版本速度快，适合实时翻译</li>
 *     <li>如果翻译失败，返回原始文本（降级策略）</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
public interface OllamaService {
    /**
     * 将中文文本翻译为英文
     * 
     * <p>调用Ollama API的chat接口，使用qwen2.5:0.5b模型进行翻译
     * 
     * <p>翻译流程：
     * <ol>
     *     <li>构造翻译请求："帮我把以下内容翻译成英文：{用户输入}"</li>
     *     <li>发送到Ollama API</li>
     *     <li>接收翻译结果</li>
     *     <li>如果失败则返回原始文本</li>
     * </ol>
     * 
     * @param prompt 待翻译的中文文本（提示词或负向提示词）
     * @return 翻译后的英文文本，翻译失败时返回原始文本
     */
    String translate(String prompt);
}
