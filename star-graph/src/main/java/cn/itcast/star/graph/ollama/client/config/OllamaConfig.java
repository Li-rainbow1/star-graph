package cn.itcast.star.graph.ollama.client.config;

import cn.itcast.star.graph.ollama.client.api.OllamaApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Ollama客户端配置类
 * 
 * <p>配置Ollama API的HTTP客户端，用于提示词翻译
 * 
 * @author itcast
 * @since 1.0
 */
@Configuration
public class OllamaConfig {

    /**
     * 创建OllamaApi客户端Bean
     * 
     * @param objectMapper JSON转换器
     * @return OllamaApi接口实例
     * @throws IOException 初始化失败时抛出
     */
    @Bean
    public OllamaApi ollamaApi(ObjectMapper objectMapper) throws IOException {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(String.format("http://192.168.100.129:11434"))
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();
        
        return retrofit.create(OllamaApi.class);
    }

}
