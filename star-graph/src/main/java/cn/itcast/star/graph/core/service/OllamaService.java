package cn.itcast.star.graph.core.service;

/**
 * Ollama AI翻译服务 - 使用本地大语言模型翻译中文提示词为英文
 */
public interface OllamaService {
    /**
     * 翻译中文为英文，失败时返回原文
     */
    String translate(String prompt);
}
