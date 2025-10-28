package cn.itcast;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 星图项目应用启动类
 * 
 * <p>星图(Star Graph)是一个基于AI的文生图服务平台，主要功能包括：
 * <ul>
 *     <li>文生图任务管理（创建、取消、查询、插队）</li>
 *     <li>用户积分管理</li>
 *     <li>任务队列调度</li>
 *     <li>与ComfyUI的集成对接</li>
 * </ul>
 * 
 * <p>技术栈：
 * <ul>
 *     <li>Spring Boot - Web框架</li>
 *     <li>MyBatis Plus - ORM框架</li>
 *     <li>Redis - 任务队列和缓存</li>
 *     <li>WebSocket - 实时消息推送</li>
 *     <li>Retrofit - HTTP客户端（调用ComfyUI）</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
@SpringBootApplication
@EnableScheduling  // 启用定时任务，用于处理任务队列
@MapperScan("cn.itcast.star.graph.core.mapper")  // 扫描Mapper接口
public class StarGraphApp {
    /**
     * 应用程序入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(StarGraphApp.class, args);
    }
}
