package cn.itcast.star.graph.core.config;

import cn.itcast.star.graph.core.job.RunTaskJob;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 信号量初始化配置 - 应用启动时初始化信号量，控制ComfyUI并发任务数
 */
@Component
@Log4j2
public class SemaphoreInitConfig implements CommandLineRunner {
    
    @Autowired
    private RedissonClient redissonClient;
    
    // 最大并发任务数（根据GPU性能调整）
    private static final int MAX_PERMITS = 1;
    
    @Override
    public void run(String... args) throws Exception {
        RSemaphore semaphore = redissonClient.getSemaphore(RunTaskJob.TASK_RUN_SEMAPHORE);
        
        // trySetPermits：仅在信号量不存在时设置，防止多实例重复初始化
        boolean isSet = semaphore.trySetPermits(MAX_PERMITS);
        
        if (isSet) {
            log.info("✅ 信号量初始化成功: key={}, 初始许可数={}", RunTaskJob.TASK_RUN_SEMAPHORE, MAX_PERMITS);
        } else {
            int currentPermits = semaphore.availablePermits();
            log.info("⚠️ 信号量已存在: key={}, 当前许可数={}", RunTaskJob.TASK_RUN_SEMAPHORE, currentPermits);
        }
    }
}
