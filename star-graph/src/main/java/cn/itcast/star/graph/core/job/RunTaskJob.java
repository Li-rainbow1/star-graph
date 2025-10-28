package cn.itcast.star.graph.core.job;

import cn.hutool.core.date.DateTime;
import cn.itcast.star.graph.comfyui.client.api.ComfyuiApi;
import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiTask;
import cn.itcast.star.graph.core.service.RedisService;
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
 * 任务调度定时任务
 * 
 * <p>负责从Redis队列中取出文生图任务并提交给ComfyUI执行
 * 
 * <p>主要功能：
 * <ul>
 *     <li>每秒轮询一次Redis队列</li>
 *     <li>使用分布式锁防止并发执行</li>
 *     <li>使用信号量控制并发任务数量</li>
 *     <li>提交任务到ComfyUI</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
@Component
@Log4j2
public class RunTaskJob {
    /** 定时任务分布式锁key */
    final static String SPRING_TASK_LOCK_KEY = "SPRING_TASK_LOCK_KEY";
    
    /** 任务运行信号量key，用于控制并发执行的任务数 */
    public final static String TASK_RUN_SEMAPHORE = "TASK_RUN_SEMAPHORE";

    @Autowired
    RedisService redisService;
    @Autowired
    ComfyuiApi comfyuiApi;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    UserFundRecordService userFundRecordService;

    /**
     * 把任务发送给ComfyUI
     * 
     * <p>执行流程：
     * <ol>
     *     <li>从Redis队列取出优先级最高的任务</li>
     *     <li>调用ComfyUI API提交任务</li>
     *     <li>成功：保存promptId到已启动任务列表</li>
     *     <li>失败：释放信号量并归还冻结积分</li>
     * </ol>
     */
    private void sendTaskToComfyui() {
        // 从Redis优先级队列中弹出优先级最高的任务（分值最小的任务）
        ComfyuiTask comfyuiTask = redisService.popQueueTask();
        // 如果队列为空或数据异常，释放信号量后返回
        if (comfyuiTask == null) {
            // 【Bug修复】必须释放已获取的信号量，否则会导致信号量永久泄露
            RSemaphore semaphore = redissonClient.getSemaphore(TASK_RUN_SEMAPHORE);
            semaphore.release();
            log.warn("从队列获取任务失败（队列为空或数据异常），释放信号量");
            return;
        }
        // 调用ComfyUI API的addQueueTask接口，创建Retrofit的Call对象
        Call<HashMap> hashMapCall = comfyuiApi.addQueueTask(comfyuiTask.getComfyuiRequestDto());
        try {
            // 同步执行HTTP请求，等待ComfyUI响应
            Response<HashMap> response = hashMapCall.execute();
            // 判断HTTP响应是否成功（状态码2xx）
            if (response.isSuccessful()) {
                // 获取响应体数据
                HashMap body = response.body();
                // 【Bug修复】添加空值检查
                if (body == null || body.get("prompt_id") == null) {
                    log.error("ComfyUI响应数据异常，body或prompt_id为null");
                    // 获取任务运行信号量
                    RSemaphore semaphore = redissonClient.getSemaphore(TASK_RUN_SEMAPHORE);
                    // 释放信号量许可
                    semaphore.release();
                    // 任务失败，归还用户冻结的积分
                    userFundRecordService.freezeReturn(comfyuiTask.getUserId(), comfyuiTask.getSize());
                    // 删除临时占位符
                    redisService.removeStartedTask("temp_" + comfyuiTask.getId());
                    return;
                }
                // 从响应中提取ComfyUI分配的任务ID（promptId）
                String promptId = (String) body.get("prompt_id");
                // 将promptId设置到任务对象中
                comfyuiTask.setPromptId(promptId);
                // 记录任务提交成功日志
                log.info("添加任务到Comfyui成功：{}", comfyuiTask.getPromptId());
                // 将任务保存到"已启动任务"列表，用于后续接收ComfyUI的执行结果
                redisService.addStartedTask(promptId, comfyuiTask);
            } else {
                // 提交失败，获取错误信息
                String error = response.errorBody().string();
                // 记录错误日志
                log.error("添加任务到Comfyui错误: {}", error);
                // 获取任务运行信号量
                RSemaphore semaphore = redissonClient.getSemaphore(TASK_RUN_SEMAPHORE);
                // 释放信号量许可，因为任务提交失败，需要归还许可供其他任务使用
                semaphore.release();
                // 任务失败，归还用户冻结的积分（按图片数量）
                userFundRecordService.freezeReturn(comfyuiTask.getUserId(), comfyuiTask.getSize());
                // 删除临时占位符，保证数据一致性
                redisService.removeStartedTask("temp_" + comfyuiTask.getId());
            }
        } catch (Exception e) {
            // 捕获异常并记录详细日志
            log.error("提交任务到Comfyui发生异常: {}", e.getMessage(), e);
            // 【重要】发生异常时必须释放信号量，否则会导致信号量泄露，后续任务无法执行
            RSemaphore semaphore = redissonClient.getSemaphore(TASK_RUN_SEMAPHORE);
            semaphore.release();
            // 任务失败，归还用户冻结的积分（comfyuiTask在此时一定不为null）
            userFundRecordService.freezeReturn(comfyuiTask.getUserId(), comfyuiTask.getSize());
            // 删除临时占位符，保证数据一致性
            redisService.removeStartedTask("temp_" + comfyuiTask.getId());
        }
    }


    /**
     * 定时任务调度方法
     * 
     * <p>每秒执行一次，检查队列中是否有任务需要处理
     * 
     * <p>使用分布式锁保证在集群环境下只有一个实例执行
     * <p>使用信号量控制同时运行的任务数量，防止ComfyUI过载
     */
    @Scheduled(cron = "*/1 * * * * ?")
    public void task() {
        // 获取分布式锁，确保在集群环境下只有一个节点执行定时任务
        RLock lock = redissonClient.getLock(SPRING_TASK_LOCK_KEY);
        // 尝试获取锁（非阻塞），如果获取失败则本次不执行
        if (lock.tryLock()) {
            try {
                // 检查Redis队列中是否有待处理的任务
                if (redisService.hasQueueTask()) {
                    // 获取任务运行信号量，用于控制ComfyUI的并发任务数
                    RSemaphore semaphore = redissonClient.getSemaphore(TASK_RUN_SEMAPHORE);
                    // 打印当前可用的信号量许可数（用于监控和调试）
                    System.out.println(DateTime.now() + "\t\t获取许可数量" + semaphore.availablePermits());
                    // 尝试获取一个信号量许可（非阻塞）
                    // 如果获取成功，说明ComfyUI还有空闲资源可以执行新任务
                    if (semaphore.tryAcquire()) {
                        // 打印任务开始执行的时间
                        System.out.println("===开关开启：>" + DateTime.now());
                        // 从队列取出任务并提交给ComfyUI执行
                        sendTaskToComfyui();
                    }
                    // 如果获取信号量失败，说明ComfyUI正在执行的任务数已达上限
                    // 本次不执行，等待下一次定时任务触发
                }
            } finally {
                // 【Bug修复】检查当前线程是否持有锁，避免抛出IllegalMonitorStateException
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
        // 如果获取分布式锁失败，说明其他节点正在执行，本次跳过

    }
}
