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

@Service
public class OllamaServiceImpl implements OllamaService {
    @Autowired
    OllamaApi ollamaApi;

    @Override
    public String translate(String prompt) {
        try {
            // 构造Ollama消息对象
            OllamaMessage ollamaMessage = new OllamaMessage();
            // 设置角色为用户（user）
            ollamaMessage.setRole("user");
            // 设置消息内容：要求Ollama将中文翻译成英文
            ollamaMessage.setContent("帮我把以下内容翻译成英文:" + prompt);
            // 构造Ollama请求对象
            OllamaChatRequest body = new OllamaChatRequest();
            // 设置使用的模型为qwen2.5:0.5b（通义千问2.5，0.5B参数版本，速度快）
            body.setModel("qwen2.5:0.5b");
            // 设置消息列表（只包含一条翻译请求消息）
            body.setMessages(List.of(ollamaMessage));
            // 调用Ollama API的chat接口，创建Retrofit的Call对象
            Call<OllamaChatRespone> chat = ollamaApi.chat(body);
            // 同步执行HTTP请求，等待Ollama响应
            Response<OllamaChatRespone> result = chat.execute();
            // 从响应体中获取Ollama的回复
            OllamaChatRespone ollamaChatRespone = result.body();
            // 提取并返回翻译后的英文内容
            return ollamaChatRespone.getMessage().getContent();
        } catch (Exception e) {
            // 如果翻译失败（网络错误、Ollama服务不可用等），打印异常
            e.printStackTrace();
        }

        // 翻译失败时，返回原始文本（降级策略）
        return prompt;
    }
}
