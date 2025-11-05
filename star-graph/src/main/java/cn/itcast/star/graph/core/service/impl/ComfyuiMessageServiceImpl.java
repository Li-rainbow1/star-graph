package cn.itcast.star.graph.core.service.impl;

import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiTask;
import cn.itcast.star.graph.comfyui.client.pojo.MessageBase;
import cn.itcast.star.graph.core.job.RunTaskJob;
import cn.itcast.star.graph.core.service.*;
import com.alibaba.fastjson2.JSON;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ComfyUI消息处理服务实现 - 处理进度、结果、错误等WebSocket消息
 */
@Slf4j
@Service
public class ComfyuiMessageServiceImpl implements ComfyuiMessageService {
    @Autowired
    WsNoticeService wsNoticeService;
    @Autowired
    RedisService redisService;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    UserResultService userResultService;
    @Autowired
    UserFundRecordService userFundRecordService;
    @Autowired
    RefundCompensationService refundCompensationService;

    @Override
    public void handleMessage(MessageBase messageBase) {
        // 根据ComfyUI推送的消息类型进行路由分发
        if ("progress".equals(messageBase.getType())) {
            handleProgressMessage(messageBase);
        } else if ("executed".equals(messageBase.getType())) {
            handleExecutedMessage(messageBase);
        } else if("execution_error".equals(messageBase.getType())){
            handleExecutionErrorMessage(messageBase);
        } else if ("execution_interrupted".equals(messageBase.getType())){
            handleExecutionInterruptedMessage(messageBase);
        } else if ("status".equals(messageBase.getType())){
            handleStatusMessage(messageBase);
        }
    }

    /**
     * 处理ComfyUI状态消息
     */
    private void handleStatusMessage(MessageBase messageBase) {
        HashMap<String, Object> data = messageBase.getData();
        if (data == null) {
            return;
        }
        HashMap<String, Object> status = (HashMap<String, Object>) data.get("status");
        if (status == null) {
            return;
        }
        HashMap<String, Object> execInfo = (HashMap<String, Object>) status.get("exec_info");
        if (execInfo == null) {
            return;
        }
        Integer queueRemaining = (Integer) execInfo.get("queue_remaining");
        // 记录ComfyUI就绪队列剩余任务数，便于观察外部队列压力
        log.debug("ComfyUI队列剩余任务数: {}", queueRemaining);
    }

    /**
     * 处理任务执行失败消息
     * 
     * @param messageBase 错误消息对象
     */
    private void handleExecutionErrorMessage(MessageBase messageBase) {
        HashMap<String, Object> data = messageBase.getData();
        if (data == null || data.get("prompt_id") == null) {
            log.warn("收到异常的error消息，prompt_id为null");
            return;
        }
        String promptId = data.get("prompt_id").toString();
        ComfyuiTask task = redisService.getStartedTask(promptId);
        
        releaseRunSemaphore("任务失败，");
        
        if(task==null){
            // 任务可能已经过期或被清理，直接返回避免NPE
            log.warn("收到失败消息，但任务已不存在（可能超时过期）: {}", promptId);
            return;
        }
        
        data.put("type","execution_error");
        // 归还冻结的积分（失败不扣费）
        refundCompensationService.safeRefund(task.getUserId(), task.getSize(), 
                promptId, "execution_error_refund_failed");
        redisService.removeStartedTask(promptId);
        // 通过WebSocket通知用户错误信息
        wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(data));
    }

    /**
     * 处理任务中断消息（用户主动取消）
     * 
     * @param messageBase 中断消息对象
     */
    private void handleExecutionInterruptedMessage(MessageBase messageBase) {
        HashMap<String, Object> data = messageBase.getData();
        if (data == null || data.get("prompt_id") == null) {
            log.warn("收到异常的interrupted消息，prompt_id为null");
            return;
        }
        String promptId = data.get("prompt_id").toString();
        ComfyuiTask task = redisService.getStartedTask(promptId);
        
        // 【关键】释放信号量，允许新任务提交
        releaseRunSemaphore("任务中断，");
        
        if(task==null){
            // 任务可能已经过期或被清理，直接返回避免NPE
            log.warn("收到中断消息，但任务已不存在（可能超时过期）: {}", promptId);
            return;
        }
        
        // 注意：积分退款已在cancelTask方法中处理，这里只需清理任务记录
        redisService.removeStartedTask(promptId);
        
        // 构造中断消息通知前端
        data.put("type","execution_interrupted");
        // 通过WebSocket通知用户任务已中断
        wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(data));
        
        log.info("任务中断完成，promptId: {}", promptId);
    }

    /**
     * 推送生图结果
     * 
     * @param messageBase 任务完成消息对象
     */
    private void handleExecutedMessage(MessageBase messageBase) {
        HashMap<String, Object> data = messageBase.getData();
        
        HashMap<String, Object> output = (HashMap<String, Object>) data.get("output");
        if (output == null) {
            log.warn("收到异常的executed消息，output字段为null");
            return;
        }
        
        List<HashMap<String, Object>> images = (List<HashMap<String, Object>>) output.get("images");
        if (images == null || images.isEmpty()) {
            log.warn("收到异常的executed消息，images字段为null或为空");
            return;
        }
        // 将图片元数据转换为可访问的URL列表（根据ComfyUI静态服务地址拼接）
        List<String> urls = images.stream().map((image) -> String.format("http://192.168.100.129:8188/view?filename=%s&type=%s&subfolder=", image.get("filename"), image.get("type")))
                .collect(Collectors.toList());
        HashMap<String, Object> temp = new HashMap<>();
        temp.put("type", "imageResult");
        temp.put("urls", urls);
        if (data.get("prompt_id") == null) {
            log.warn("收到异常的executed消息，prompt_id为null");
            return;
        }
        String promptId = data.get("prompt_id").toString();
        ComfyuiTask task = redisService.getStartedTask(promptId);
        
        releaseRunSemaphore("任务完成，");
        
        if (task == null) {
            // 任务丢失场景下无需继续业务处理
            log.warn("收到完成消息，但任务已不存在（可能超时过期）: {}", promptId);
            return;
        }
        
        // 任务成功完成，扣除积分（从冻结账户转到系统账户）
        userFundRecordService.pointsDeduction(task.getUserId(), task.getSize());
        log.info("扣除用户{}的积分: {}", task.getUserId(), task.getSize());
        
        // 保存生成的图片到数据库
        userResultService.saveList(urls,task.getUserId());
        redisService.removeStartedTask(promptId);
        // 通过WebSocket推送结果给用户
        wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(temp));
    }

    /**
     * 处理任务执行进度消息
     */
    private void handleProgressMessage(MessageBase messageBase) {
        HashMap<String, Object> data = messageBase.getData();
        if (data == null || data.get("prompt_id") == null) {
            return;
        }
        String promptId = data.get("prompt_id").toString();
        ComfyuiTask task = redisService.getStartedTask(promptId);
        data.put("type", "progress");
        if (task == null) {
            // 任务不存在时丢弃进度消息，避免推送到无效连接
            return;
        }
        // 将进度消息推送给对应的WebSocket客户端
        wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(data));
    }

    private void releaseRunSemaphore(String prefix) {
        RSemaphore semaphore = redissonClient.getSemaphore(RunTaskJob.TASK_RUN_SEMAPHORE);
        semaphore.release();
        log.info("{}释放信号量，当前许可数: {}", prefix, semaphore.availablePermits());
    }
}
