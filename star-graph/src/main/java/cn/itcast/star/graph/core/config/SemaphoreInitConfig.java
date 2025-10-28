package cn.itcast.star.graph.core.config;

import cn.itcast.star.graph.core.job.RunTaskJob;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 信号量初始化配置类
 * 
 * <p>在应用启动时初始化分布式信号量的许可数量
 * 
 * <p>作用：控制ComfyUI的并发任务数量，防止GPU显存溢出
 * 
 * @author itcast
 * @since 1.0
 */
@Component
@Log4j2
public class SemaphoreInitConfig implements CommandLineRunner {
    
    @Autowired
    private RedissonClient redissonClient;
    
    /**
     * 信号量的最大许可数（控制ComfyUI同时执行的任务数量）
     * 根据ComfyUI服务器性能调整：
     * - 1: 单任务串行执行（适合单GPU或显存较小的情况）
     * - 10: 允许10个任务并发（适合多GPU或显存充足的情况）
     */
    private static final int MAX_PERMITS = 1;
    
    @Override
    public void run(String... args) throws Exception {
        // 获取任务运行信号量
        RSemaphore semaphore = redissonClient.getSemaphore(RunTaskJob.TASK_RUN_SEMAPHORE);
        
        // trySetPermits()：仅在信号量不存在时设置初始许可数
        // 如果Redis中已存在该信号量，则不会重置（防止多实例启动时重复初始化）
        boolean isSet = semaphore.trySetPermits(MAX_PERMITS);
        
        if (isSet) {
            log.info("✅ 信号量初始化成功: key={}, 初始许可数={}", RunTaskJob.TASK_RUN_SEMAPHORE, MAX_PERMITS);
        } else {
            // 信号量已存在，打印当前许可数
            int currentPermits = semaphore.availablePermits();
            log.info("⚠️ 信号量已存在: key={}, 当前许可数={}", RunTaskJob.TASK_RUN_SEMAPHORE, currentPermits);
        }
    }
}
