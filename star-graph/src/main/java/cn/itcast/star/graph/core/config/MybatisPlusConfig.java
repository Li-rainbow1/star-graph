package cn.itcast.star.graph.core.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus配置类
 * 
 * <p>配置MyBatis Plus的拦截器插件，提供以下功能：
 * <ul>
 *     <li>分页查询支持</li>
 *     <li>乐观锁支持</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
@Configuration
public class MybatisPlusConfig {
    /**
     * 配置MyBatis Plus拦截器
     * 
     * <p>添加以下内置拦截器：
     * <ul>
     *     <li>分页拦截器：自动处理分页查询</li>
 *     <li>乐观锁拦截器：基于version字段实现乐观锁</li>
     * </ul>
     * 
     * @return 配置好的MyBatis Plus拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // 乐观锁
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
