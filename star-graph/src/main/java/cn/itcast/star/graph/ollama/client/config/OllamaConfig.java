// 定义包路径
package cn.itcast.star.graph.ollama.client.config;

// 导入自定义的Ollama API接口，定义了与Ollama服务交互的方法
import cn.itcast.star.graph.ollama.client.api.OllamaApi;
// 导入Jackson的ObjectMapper，用于JSON序列化和反序列化
import com.fasterxml.jackson.databind.ObjectMapper;
// 导入OkHttp客户端，用于执行HTTP请求
import okhttp3.OkHttpClient;
// 导入OkHttp的HTTP日志拦截器，用于记录请求和响应日志
import okhttp3.logging.HttpLoggingInterceptor;
// 导入Spring的Bean注解，用于声明Spring管理的Bean
import org.springframework.context.annotation.Bean;
// 导入Spring的Configuration注解，标识这是一个配置类
import org.springframework.context.annotation.Configuration;
// 导入Retrofit核心类，用于构建RESTful API客户端
import retrofit2.Retrofit;
// 导入Retrofit的Jackson转换器工厂，用于将JSON自动转换为Java对象
import retrofit2.converter.jackson.JacksonConverterFactory;

// 导入IOException异常类
import java.io.IOException;
// 导入时间单位枚举类，用于设置超时时间
import java.util.concurrent.TimeUnit;

/**
 * Ollama客户端配置类
 * 
 * <p>配置Ollama API的HTTP客户端，使用Retrofit框架进行RESTful API调用
 * 
 * <p>主要配置：
 * <ul>
 *     <li>HTTP客户端：OkHttp，支持连接池、超时、重试</li>
 *     <li>日志拦截器：记录请求和响应的详细信息，便于调试</li>
 *     <li>JSON转换：使用Jackson自动转换JSON和Java对象</li>
 *     <li>基础URL：Ollama服务的访问地址（192.168.100.129:11434）</li>
 * </ul>
 * 
 * <p>Ollama简介：
 * <ul>
 *     <li>Ollama是本地部署的大语言模型服务</li>
 *     <li>支持多种模型：qwen2.5、llama3、mistral等</li>
 *     <li>提供标准的RESTful API接口</li>
 *     <li>用于提示词翻译等AI功能</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
// 标识这是一个Spring配置类，Spring启动时会扫描并加载其中的Bean
@Configuration
public class OllamaConfig {

    /**
     * 创建OllamaApi客户端Bean
     * 
     * <p>使用Retrofit构建Ollama API的HTTP客户端，配置日志记录、超时、重试等特性
     * 
     * <p>配置细节：
     * <ul>
     *     <li>HTTP日志级别：BODY（记录完整的请求和响应体）</li>
     *     <li>连接超时：30秒</li>
     *     <li>失败重试：开启</li>
     *     <li>JSON转换：Jackson自动序列化/反序列化</li>
     * </ul>
     * 
     * @param objectMapper Spring容器中的ObjectMapper实例，用于JSON转换
     * @return OllamaApi接口的代理实现，可直接调用Ollama API
     * @throws IOException 初始化失败时抛出
     */
    @Bean  // 将返回值注册为Spring容器中的Bean，供其他组件注入使用
    public OllamaApi ollamaApi(ObjectMapper objectMapper) throws IOException {
        // 创建HTTP日志拦截器实例，用于记录HTTP请求和响应的详细信息
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        // 设置日志级别为BODY，记录完整的请求体和响应体（包含JSON数据）
        // 其他级别：NONE（不记录）、BASIC（只记录请求行和响应行）、HEADERS（记录请求行+响应行+头部）
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 使用Builder模式构建OkHttpClient实例，配置HTTP客户端的各项参数
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // 添加日志拦截器到OkHttp拦截器链，拦截所有请求和响应
                .addInterceptor(loggingInterceptor)
                // 开启连接失败重试机制，网络不稳定时自动重试
                .retryOnConnectionFailure(true)
                // 设置连接超时时间为30秒，超过此时间未建立连接则抛出超时异常
                .connectTimeout(30, TimeUnit.SECONDS)
                // 完成构建，返回配置好的OkHttpClient实例
                .build();
        
        // 使用Builder模式构建Retrofit实例，配置RESTful API客户端
        Retrofit retrofit = new Retrofit.Builder()
                // 设置Ollama服务的基础URL地址（IP:192.168.100.129，端口:11434）
                // 所有API请求都会以此为基础路径
                .baseUrl(String.format("http://192.168.100.129:11434"))
                // 设置底层HTTP客户端为前面配置好的OkHttpClient
                .client(okHttpClient)
                // 添加JSON转换器工厂，使用Jackson进行JSON和Java对象的自动转换
                // create(objectMapper)使用Spring容器中的ObjectMapper实例
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                // 完成构建，返回配置好的Retrofit实例
                .build();
        
        // 使用Retrofit创建OllamaApi接口的动态代理实现
        // Retrofit会根据接口中的注解自动生成HTTP请求代码
        OllamaApi ollamaApi = retrofit.create(OllamaApi.class);
        
        // 返回创建好的OllamaApi实例，注册到Spring容器供其他组件使用
        return ollamaApi;
    }

}
