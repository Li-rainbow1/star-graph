package cn.itcast.star.graph.core.config;

import cn.itcast.star.graph.core.interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 
 * <p>注册用户认证拦截器，拦截所有请求并验证JWT token
 * 
 * @author itcast
 * @since 1.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * 添加拦截器
     * 
     * <p>排除登录接口，其他接口需要token认证
     * 
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserInterceptor())
                .excludePathPatterns("/api/1.0/user/login");
    }
}
