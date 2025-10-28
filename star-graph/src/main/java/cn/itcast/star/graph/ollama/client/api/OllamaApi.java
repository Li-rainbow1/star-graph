package cn.itcast.star.graph.ollama.client.api;

import cn.itcast.star.graph.ollama.client.pojo.OllamaChatRequest;
import cn.itcast.star.graph.ollama.client.pojo.OllamaChatRespone;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OllamaApi {

    /**
     * 获取队列中的任务信息
     * @return
     */
    @POST("/api/chat")
    Call<OllamaChatRespone> chat(@Body OllamaChatRequest body);

}
