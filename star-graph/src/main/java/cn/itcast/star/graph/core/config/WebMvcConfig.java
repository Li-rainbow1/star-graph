// 定义包路径
package cn.itcast.star.graph.core.config;

// 导入自定义的用户认证拦截器
import cn.itcast.star.graph.core.interceptor.UserInterceptor;
// 导入Spring的Configuration注解，标识这是一个配置类
import org.springframework.context.annotation.Configuration;
// 导入Spring MVC的拦截器注册器接口
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
// 导入Spring MVC的配置器接口，用于自定义MVC配置
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 
 * <p>实现WebMvcConfigurer接口，自定义Spring MVC的行为
 * 
 * <p>主要功能：
 * <ul>
 *     <li>注册用户认证拦截器（UserInterceptor）</li>
 *     <li>配置拦截规则（拦截哪些路径、排除哪些路径）</li>
 *     <li>实现统一的用户认证和权限控制</li>
 * </ul>
 * 
 * <p>拦截器工作流程：
 * <ol>
 *     <li>客户端发送请求到Controller</li>
 *     <li>拦截器preHandle()方法执行，验证JWT token</li>
 *     <li>如果token有效，放行请求；否则返回401</li>
 *     <li>Controller执行业务逻辑</li>
 *     <li>拦截器afterCompletion()方法执行，清理ThreadLocal</li>
 * </ol>
 * 
 * <p>排除路径说明：
 * <ul>
 *     <li>/api/1.0/user/login：登录接口，无需token认证</li>
 *     <li>可根据需要添加其他排除路径（如注册、公开接口等）</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 * @see UserInterceptor 用户认证拦截器实现
 */
// 标识这是一个Spring配置类，会被Spring容器扫描并加载
@Configuration
// 实现WebMvcConfigurer接口，用于自定义Spring MVC配置
// 注意：Spring Boot 2.0+推荐实现WebMvcConfigurer接口，而不是继承WebMvcConfigurerAdapter
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * 添加拦截器配置方法
     * 
     * <p>重写WebMvcConfigurer接口的addInterceptors方法，
     * 向Spring MVC注册自定义的拦截器
     * 
     * <p>拦截器配置说明：
     * <ul>
     *     <li>拦截路径：默认拦截所有路径（/**）</li>
     *     <li>排除路径：/api/1.0/user/login（登录接口）</li>
     *     <li>执行顺序：按照添加顺序执行，如有多个拦截器可设置order</li>
     * </ul>
     * 
     * <p>拦截器链执行顺序：
     * <pre>
     * 请求 → 拦截器1.preHandle → 拦截器2.preHandle → Controller
     *       ← 拦截器1.afterCompletion ← 拦截器2.afterCompletion ← 响应
     * </pre>
     * 
     * @param registry 拦截器注册器，用于注册和配置拦截器
     */
    @Override  // 重写父接口的方法
    public void addInterceptors(InterceptorRegistry registry) {
        // 向注册器添加用户认证拦截器
        // registry.addInterceptor()：注册一个拦截器
        // new UserInterceptor()：创建拦截器实例（也可以通过@Bean注入）
        // excludePathPatterns()：排除指定路径，这些路径不会被拦截
        // "/api/1.0/user/login"：登录接口路径，用户未登录时需要访问此接口获取token，因此需要排除
        registry.addInterceptor(new UserInterceptor())
                .excludePathPatterns("/api/1.0/user/login");
        
        // 【可选配置示例】如果需要更精细的控制，可以使用以下配置：
        // .addPathPatterns("/**")  // 明确指定拦截所有路径
        // .excludePathPatterns("/api/1.0/user/login", "/api/1.0/user/register")  // 排除多个路径
        // .order(1)  // 设置拦截器执行顺序（数字越小优先级越高）
    }
}
