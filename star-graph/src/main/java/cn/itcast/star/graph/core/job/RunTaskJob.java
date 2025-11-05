package cn.itcast.star.graph.core.job;

import cn.itcast.star.graph.comfyui.client.api.ComfyuiApi;
import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiTask;
import cn.itcast.star.graph.core.service.RedisService;
import cn.itcast.star.graph.core.service.RefundCompensationService;
import cn.itcast.star.graph.core.service.UserFundRecordService;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.util.HashMap;

/**
 * 任务调度定时任务 - 每秒从Redis队列取任务提交给ComfyUI
 * 使用分布式锁防止集群重复执行，使用信号量控制并发数
 */
@Component
@Log4j2
public class RunTaskJob {
    final static String SPRING_TASK_LOCK_KEY = "SPRING_TASK_LOCK_KEY";
    public final static String TASK_RUN_SEMAPHORE = "TASK_RUN_SEMAPHORE";

    @Autowired
    RedisService redisService;
    @Autowired
    ComfyuiApi comfyuiApi;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    UserFundRecordService userFundRecordService;
    @Autowired
    RefundCompensationService refundCompensationService;

    /**
     * 释放信号量
     */
    private void releaseSemaphore(String reason) {
        RSemaphore semaphore = redissonClient.getSemaphore(TASK_RUN_SEMAPHORE);
        semaphore.release();
        log.info("{}, 释放信号量，当前可用许可: {}", reason, semaphore.availablePermits());
    }
    
    /**
     * 从队列弹出任务并提交到ComfyUI
     * 1. 从Redis队列中弹出任务
     * 2. 提交任务到ComfyUI
     * 3. 处理ComfyUI响应结果
     */
    private void sendTaskToComfyui() {
        ComfyuiTask comfyuiTask = redisService.popQueueTask();
        if (comfyuiTask == null) {
            releaseSemaphore("从队列获取任务失败");
            return;
        }
        
        Call<HashMap> hashMapCall = comfyuiApi.addQueueTask(comfyuiTask.getComfyuiRequestDto());
        try {
            Response<HashMap> response = hashMapCall.execute();
            if (response.isSuccessful()) {
                HashMap body = response.body();
                if (body == null || body.get("prompt_id") == null) {
                    log.error("ComfyUI响应数据异常，body或prompt_id为null");
                    releaseSemaphore("ComfyUI响应数据异常");
                    refundCompensationService.safeRefund(comfyuiTask.getUserId(), comfyuiTask.getSize(), 
                            "temp_" + comfyuiTask.getId(), "comfyui_response_error_refund_failed");
                    redisService.removeStartedTask("temp_" + comfyuiTask.getId());
                    return;
                }
                String promptId = (String) body.get("prompt_id");
                comfyuiTask.setPromptId(promptId);
                log.info("添加任务到Comfyui成功：{}", comfyuiTask.getPromptId());
                // 将任务标记为“已开始执行”，用于后续WS消息匹配、排名计算
                redisService.addStartedTask(promptId, comfyuiTask);
            } else {
                String error = response.errorBody().string();
                log.error("添加任务到Comfyui错误: {}", error);
                releaseSemaphore("ComfyUI提交失败");
                refundCompensationService.safeRefund(comfyuiTask.getUserId(), comfyuiTask.getSize(), 
                        "temp_" + comfyuiTask.getId(), "comfyui_submit_error_refund_failed");
                redisService.removeStartedTask("temp_" + comfyuiTask.getId());
            }
        } catch (Exception e) {
            log.error("提交任务到Comfyui发生异常: {}", e.getMessage(), e);
            releaseSemaphore("提交任务异常");
            refundCompensationService.safeRefund(comfyuiTask.getUserId(), comfyuiTask.getSize(), 
                    "temp_" + comfyuiTask.getId(), "comfyui_submit_exception_refund_failed");
            redisService.removeStartedTask("temp_" + comfyuiTask.getId());
        }
    }

    /**
     * 定时任务入口，每秒执行一次（获取分布式锁和信号量后执行）
     * 
     * <p>调度策略：
     * <ul>
     *     <li>使用fixedDelay而非cron：避免任务堆积，确保上次执行完成后再开始</li>
     *     <li>1秒间隔：快速响应队列中的任务</li>
     *     <li>分布式锁：防止集群环境下多实例并发执行</li>
     *     <li>信号量控制：限制并发提交到ComfyUI的任务数</li>
     * </ul>
     * 
     * <p>执行流程：
     * <ol>
     *     <li>获取分布式锁，防止集群重复执行</li>
     *     <li>快速判断是否有待处理任务，避免无任务时重复获取信号量</li>
     *     <li>通过信号量限制并发提交到ComfyUI的任务数，获取失败则跳过本轮</li>
     * </ol>
     */
    @Scheduled(fixedDelay = 1000)  // 上次执行完成后延迟1秒
    public void task() {
        // 使用分布式锁，保证同一时间仅有一个实例执行取队列与提交逻辑
        RLock lock = redissonClient.getLock(SPRING_TASK_LOCK_KEY);
        if (lock.tryLock()) {
            try {
                // 快速判断是否有待处理任务，避免无任务时重复获取信号量
                if (redisService.hasQueueTask()) {
                    RSemaphore semaphore = redissonClient.getSemaphore(TASK_RUN_SEMAPHORE);
                    log.debug("当前可用信号量: {}", semaphore.availablePermits());
                    // 通过信号量限制并发提交到ComfyUI的任务数，获取失败则跳过本轮
                    if (semaphore.tryAcquire()) {
                        log.debug("获取信号量成功，开始处理任务");
                        // 已获得许可，本轮负责从队列弹出并提交任务
                        sendTaskToComfyui();
                    }
                }
            } finally {
                // 仅释放当前线程持有的锁，避免误释放
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}
