package cn.itcast.star.graph.comfyui.client.config;

import cn.itcast.star.graph.comfyui.client.api.ComfyuiApi;
import cn.itcast.star.graph.comfyui.client.handler.ComfyuiMessageHandler;
import cn.itcast.star.graph.core.common.Constants;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Duration;

/**
 * ComfyUI客户端配置 - 配置HTTP API和WebSocket连接
 */
@Configuration
public class ComfyuiConfig {

    /**
     * 配置ComfyUI HTTP API（使用Retrofit，超时30秒）
     */
    @Bean
    public ComfyuiApi comfyuiApi() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .callTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.100.129:8188/")
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        
        return retrofit.create(ComfyuiApi.class);
    }

    /**
     * 配置ComfyUI WebSocket连接，接收实时推送消息（进度、结果、错误）
     */
    @Bean
    public WebSocketConnectionManager webSocketConnectionManager(ComfyuiMessageHandler comfyuiMessageHandler) {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        String url = "ws://192.168.100.129:8188/ws?clientId=" + Constants.COMFYUI_CLIENT_ID;
        WebSocketConnectionManager manager = new WebSocketConnectionManager(webSocketClient, comfyuiMessageHandler, url);
        manager.start();
        return manager;
    }
}
