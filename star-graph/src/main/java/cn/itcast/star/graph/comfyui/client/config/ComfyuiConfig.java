// 定义包路径
package cn.itcast.star.graph.comfyui.client.config;

// 导入ComfyUI API接口，定义了与ComfyUI服务交互的REST API方法
import cn.itcast.star.graph.comfyui.client.api.ComfyuiApi;
// 导入ComfyUI WebSocket消息处理器，处理接收到的实时消息
import cn.itcast.star.graph.comfyui.client.handler.ComfyuiMessageHandler;
// 导入常量类，包含ComfyUI客户端ID等配置
import cn.itcast.star.graph.core.common.Constants;
// 导入OkHttp客户端，用于执行HTTP请求
import okhttp3.OkHttpClient;
// 导入OkHttp的HTTP日志拦截器，用于记录请求和响应日志
import okhttp3.logging.HttpLoggingInterceptor;
// 导入Spring的Bean注解，用于声明Spring管理的Bean
import org.springframework.context.annotation.Bean;
// 导入Spring的Configuration注解，标识这是一个配置类
import org.springframework.context.annotation.Configuration;
// 导入Spring WebSocket客户端接口
import org.springframework.web.socket.client.WebSocketClient;
// 导入Spring WebSocket连接管理器，管理WebSocket连接的生命周期
import org.springframework.web.socket.client.WebSocketConnectionManager;
// 导入Spring标准WebSocket客户端实现（基于JSR-356）
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
// 导入Retrofit核心类，用于构建RESTful API客户端
import retrofit2.Retrofit;
// 导入Retrofit的Jackson转换器工厂，用于JSON序列化和反序列化
import retrofit2.converter.jackson.JacksonConverterFactory;

// 导入Java 8的Duration类，用于设置超时时间
import java.time.Duration;

/**
 * ComfyUI客户端配置类
 * 
 * <p>负责配置与ComfyUI服务的HTTP和WebSocket连接
 * 
 * <p>两种连接方式：
 * <ul>
 *     <li>HTTP连接：用于同步调用REST API（提交任务、查询状态、获取结果、中断任务等）</li>
 *     <li>WebSocket连接：用于接收异步推送消息（任务进度、执行结果、错误通知等）</li>
 * </ul>
 * 
 * <p>技术栈：
 * <ul>
 *     <li>Retrofit：类型安全的HTTP客户端框架，简化REST API调用</li>
 *     <li>OkHttp：底层HTTP网络库，支持连接池、拦截器、超时控制</li>
 *     <li>Spring WebSocket：WebSocket客户端支持，管理连接生命周期</li>
 * </ul>
 * 
 * <p>ComfyUI服务地址：
 * <ul>
 *     <li>HTTP：http://192.168.100.129:8188/</li>
 *     <li>WebSocket：ws://192.168.100.129:8188/ws</li>
 * </ul>
 * 
 * <p>连接特性：
 * <ul>
 *     <li>HTTP请求超时：30秒</li>
 *     <li>自动重试机制：连接失败时自动重试</li>
 *     <li>请求日志：记录完整的请求和响应信息，便于调试</li>
 *     <li>WebSocket持久连接：应用启动时建立，保持长连接</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
// 标识这是一个Spring配置类，会被Spring容器扫描并加载
@Configuration
// ComfyUI客户端配置类
public class ComfyuiConfig {

    /**
     * 配置ComfyUI的HTTP客户端API Bean
     * 
     * <p>使用Retrofit框架创建类型安全的HTTP客户端，用于调用ComfyUI的REST API
     * 
     * <p>API接口功能：
     * <ul>
     *     <li>提交工作流任务（POST /prompt）</li>
     *     <li>查询队列状态（GET /queue）</li>
     *     <li>中断任务执行（POST /interrupt）</li>
     *     <li>获取生成的图片（GET /view）</li>
     *     <li>查询历史记录（GET /history）</li>
     * </ul>
     * 
     * <p>客户端配置：
     * <ul>
     *     <li>HTTP日志拦截器：记录完整的请求和响应体，便于调试</li>
     *     <li>自动重试：网络故障时自动重试连接</li>
     *     <li>调用超时：整个API调用最多30秒</li>
     *     <li>读取超时：读取响应数据最多30秒</li>
     *     <li>JSON转换：使用Jackson自动处理JSON序列化</li>
     * </ul>
     * 
     * @return ComfyUI API接口的代理实现，可直接调用API方法
     */
    @Bean  // 将返回值注册为Spring容器中的Bean
    public ComfyuiApi comfyuiApi() {
        // 创建HTTP日志拦截器实例，用于记录HTTP请求和响应
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        
        // 设置日志级别为BODY，记录完整的请求体和响应体
        // BODY级别会输出：请求行、请求头、请求体、响应行、响应头、响应体
        // 其他级别：NONE（不记录）、BASIC（请求行+响应行）、HEADERS（+头部信息）
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 使用Builder模式构建OkHttpClient实例，配置HTTP客户端
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // 添加日志拦截器到拦截器链，拦截所有HTTP请求和响应
                .addInterceptor(loggingInterceptor)
                // 开启连接失败自动重试机制
                // 当网络连接失败时，OkHttp会自动重试请求
                .retryOnConnectionFailure(true)
                // 设置整个API调用的超时时间为30秒
                // 包括：建立连接、发送请求、接收响应的总时间
                // Duration.ofSeconds(30)创建一个30秒的时间段
                .callTimeout(Duration.ofSeconds(30))
                // 设置读取响应数据的超时时间为30秒
                // 从服务器读取响应数据最多等待30秒
                .readTimeout(Duration.ofSeconds(30))
                // 完成构建，返回配置好的OkHttpClient实例
                .build();

        // 使用Builder模式构建Retrofit实例，配置RESTful API客户端
        Retrofit retrofit = new Retrofit.Builder()
                // 设置ComfyUI服务的基础URL地址
                // 所有API请求的URL都会以此为前缀
                // 例如：POST /prompt 实际请求地址为 http://192.168.100.129:8188/prompt
                .baseUrl("http://192.168.100.129:8188/")
                // 设置底层HTTP客户端为前面配置好的OkHttpClient
                .client(okHttpClient)
                // 添加Jackson转换器工厂，用于JSON和Java对象的自动转换
                // 请求参数自动转为JSON，响应JSON自动转为Java对象
                .addConverterFactory(JacksonConverterFactory.create())
                // 完成构建，返回配置好的Retrofit实例
                .build();
        
        // 使用Retrofit创建ComfyuiApi接口的动态代理实现
        // Retrofit会根据接口方法上的注解（如@POST、@GET）自动生成HTTP请求代码
        ComfyuiApi comfyuiApi = retrofit.create(ComfyuiApi.class);
        
        // 返回创建好的API实例，注册到Spring容器供其他组件使用
        return comfyuiApi;
    }

    /**
     * 配置ComfyUI的WebSocket连接管理器Bean
     * 
     * <p>建立与ComfyUI服务的WebSocket长连接，用于接收任务执行的实时推送消息
     * 
     * <p>接收的消息类型：
     * <ul>
     *     <li>status消息：ComfyUI队列状态变更（有任务开始执行、队列为空等）</li>
     *     <li>progress消息：任务执行进度更新（当前步数/总步数）</li>
     *     <li>executed消息：节点执行完成，包含生成的图片信息</li>
     *     <li>execution_error消息：任务执行失败，包含错误详情</li>
     * </ul>
     * 
     * <p>连接特性：
     * <ul>
     *     <li>应用启动时自动建立连接（manager.start()）</li>
     *     <li>保持长连接，持续接收消息推送</li>
     *     <li>断开后Spring会自动尝试重连</li>
     *     <li>使用clientId标识此客户端连接</li>
     * </ul>
     * 
     * <p>为什么需要clientId：
     * <ul>
     *     <li>ComfyUI通过clientId区分不同的客户端</li>
     *     <li>可以向特定客户端推送消息</li>
     *     <li>便于服务端管理多个客户端连接</li>
     * </ul>
     * 
     * @param comfyuiMessageHandler WebSocket消息处理器，由Spring自动注入，处理接收到的各类消息
     * @return WebSocket连接管理器实例，管理WebSocket连接的生命周期
     */
    @Bean  // 将返回值注册为Spring容器中的Bean
    public WebSocketConnectionManager webSocketConnectionManager(ComfyuiMessageHandler comfyuiMessageHandler) {
        // 创建标准WebSocket客户端实例
        // StandardWebSocketClient：基于JSR-356规范的WebSocket客户端实现
        // 适用于Java EE环境，兼容性好
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        
        // 构造WebSocket连接URL
        // ws://：WebSocket协议前缀
        // 192.168.100.129:8188：ComfyUI服务地址和端口
        // /ws：WebSocket端点路径
        // ?clientId=：查询参数，携带客户端唯一标识
        // Constants.COMFYUI_CLIENT_ID：从常量类获取客户端ID（固定值或UUID）
        String url = "ws://192.168.100.129:8188/ws?clientId=" + Constants.COMFYUI_CLIENT_ID;
        
        // 创建WebSocket连接管理器
        // 参数1：webSocketClient - WebSocket客户端实例
        // 参数2：comfyuiMessageHandler - 消息处理器，接收到消息时会调用其handleTextMessage方法
        // 参数3：url - WebSocket服务端地址
        WebSocketConnectionManager manager = new WebSocketConnectionManager(webSocketClient, comfyuiMessageHandler, url);
        
        // 启动连接管理器，立即建立WebSocket连接
        // start()方法会：
        // 1. 建立到ComfyUI服务的WebSocket连接
        // 2. 连接成功后调用comfyuiMessageHandler.afterConnectionEstablished()
        // 3. 开始监听服务端推送的消息
        manager.start();
        
        // 返回连接管理器实例，注册到Spring容器
        // Spring会管理其生命周期，应用关闭时自动断开连接
        return manager;
    }
}
