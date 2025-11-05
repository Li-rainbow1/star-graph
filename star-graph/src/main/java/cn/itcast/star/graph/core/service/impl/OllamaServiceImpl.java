package cn.itcast.star.graph.core.service.impl;

import cn.itcast.star.graph.core.service.OllamaService;
import cn.itcast.star.graph.ollama.client.api.OllamaApi;
import cn.itcast.star.graph.ollama.client.pojo.OllamaChatRequest;
import cn.itcast.star.graph.ollama.client.pojo.OllamaChatRespone;
import cn.itcast.star.graph.ollama.client.pojo.OllamaMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

/**
 * Ollama AI翻译服务实现
 */
@Service
public class OllamaServiceImpl implements OllamaService {
    @Autowired
    OllamaApi ollamaApi;

    /**
     * 调用Ollama翻译中文为英文，失败时返回原文
     */
    @Override
    public String translate(String prompt) {
        try {
            // 构造翻译请求
            OllamaMessage ollamaMessage = new OllamaMessage();
            ollamaMessage.setRole("user");
            ollamaMessage.setContent("帮我把以下内容翻译成英文:" + prompt);
            
            OllamaChatRequest body = new OllamaChatRequest();
            body.setModel("qwen2.5:0.5b");
            body.setMessages(List.of(ollamaMessage));
            
            // 调用Ollama API进行翻译
            Call<OllamaChatRespone> chat = ollamaApi.chat(body);
            Response<OllamaChatRespone> result = chat.execute();
            OllamaChatRespone ollamaChatRespone = result.body();
            return ollamaChatRespone.getMessage().getContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 翻译失败时返回原始文本（降级策略）
        return prompt;
    }
}
