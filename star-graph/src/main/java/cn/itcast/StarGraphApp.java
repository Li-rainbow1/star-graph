package cn.itcast;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 星图项目启动类 - AI文生图服务平台
 * 功能：文生图任务管理、积分管理、队列调度、ComfyUI集成
 * 技术栈：Spring Boot + MyBatis Plus + Redis + WebSocket
 */
@SpringBootApplication
@EnableScheduling  // 启用定时任务，用于处理任务队列
@MapperScan("cn.itcast.star.graph.core.mapper")
public class StarGraphApp {
    public static void main(String[] args) {
        SpringApplication.run(StarGraphApp.class, args);
    }
}
