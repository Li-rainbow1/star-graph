package cn.itcast.star.graph.core.service.impl;

import cn.hutool.core.date.DateTime;
import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiTask;
import cn.itcast.star.graph.comfyui.client.pojo.MessageBase;
import cn.itcast.star.graph.core.job.RunTaskJob;
import cn.itcast.star.graph.core.service.*;
import com.alibaba.fastjson2.JSON;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public void handleMessage(MessageBase messageBase) {
        // 根据消息类型分发处理
        if ("progress".equals(messageBase.getType())) {
            // 处理任务执行进度消息（如：当前步数/总步数）
            handleProgressMessage(messageBase);
        } else if ("executed".equals(messageBase.getType())) {
            // 处理任务执行完成消息（包含生成的图片信息）
            handleExecutedMessage(messageBase);
        } else if("execution_error".equals(messageBase.getType())){
            // 处理任务执行失败消息（归还积分并通知用户）
            handleExecutionErrorMessage(messageBase);
        } else if ("status".equals(messageBase.getType())){
            // 处理ComfyUI状态消息（如：队列剩余任务数）
            handleStatusMessage(messageBase);
        }
    }

    /**
     * 处理ComfyUI状态消息
     * 
     * <p>用于监控ComfyUI的队列状态，不再负责释放信号量
     * <p>信号量的释放改由任务完成(handleExecutedMessage)和失败(handleExecutionErrorMessage)时触发
     * 
     * @param messageBase 状态消息对象
     */
    private void handleStatusMessage(MessageBase messageBase) {
        // 获取消息数据
        HashMap<String, Object> data = messageBase.getData();
        if (data == null) {
            return;
        }
        // 提取status字段（包含ComfyUI当前状态信息）
        HashMap<String, Object> status = (HashMap<String, Object>) data.get("status");
        if (status == null) {
            return;
        }
        // 提取exec_info字段（包含执行信息）
        HashMap<String, Object> execInfo = (HashMap<String, Object>) status.get("exec_info");
        if (execInfo == null) {
            return;
        }
        // 获取ComfyUI队列中剩余的任务数量
        Integer queueRemaining = (Integer) execInfo.get("queue_remaining");
        
        // 仅用于监控和日志记录
        System.out.println(DateTime.now() + "\t\tComfyUI队列剩余任务数: " + queueRemaining);
    }

    /**
     * 处理任务执行失败消息
     * 
     * <p>任务失败时的处理流程：
     * <ol>
     *     <li>释放信号量许可（允许提交新任务）</li>
     *     <li>归还用户冻结的积分</li>
     *     <li>从执行中任务列表删除</li>
     *     <li>通知用户任务失败</li>
     * </ol>
     * 
     * @param messageBase 错误消息对象
     */
    private void handleExecutionErrorMessage(MessageBase messageBase) {
        // 获取错误消息数据
        HashMap<String, Object> data = messageBase.getData();
        if (data == null || data.get("prompt_id") == null) {
            System.out.println(DateTime.now() + "\t\t⚠️ 收到异常的error消息，prompt_id为null");
            return;
        }
        // 提取ComfyUI返回的任务ID（promptId）
        String promptId = data.get("prompt_id").toString();
        // 根据promptId从Redis获取对应的任务信息
        ComfyuiTask task = redisService.getStartedTask(promptId);
        
        // 【关键Bug修复】无论任务是否存在，都必须释放信号量
        // 场景：任务执行超过60分钟，Redis中的任务数据已过期删除，但信号量已被占用
        // 如果不释放，会导致信号量永久泄露
        RSemaphore semaphore = redissonClient.getSemaphore(RunTaskJob.TASK_RUN_SEMAPHORE);
        semaphore.release();
        System.out.println(DateTime.now() + "\t\t任务失败，释放信号量，当前许可数: " + semaphore.availablePermits());
        
        // 如果任务不存在（可能已超时被清理），只释放信号量，不做其他操作
        if(task==null){
            System.out.println(DateTime.now() + "\t\t⚠️ 收到失败消息，但任务已不存在（可能超时过期）: " + promptId);
            return;
        }
        
        // 设置消息类型为execution_error，方便前端识别
        data.put("type","execution_error");
        // 任务失败，归还用户冻结的积分（按生成图片数量）
        userFundRecordService.freezeReturn(task.getUserId(), task.getSize());
        // 从正在执行的任务集合中删除已失败的任务
        redisService.removeStartedTask(promptId);
        // 通过WebSocket向用户推送错误消息
        wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(data));
    }

    /**
     * 推送生图结果
     * 
     * <p>任务成功完成时的处理流程：
     * <ol>
     *     <li>释放信号量许可（允许提交新任务）</li>
     *     <li>保存图片结果到数据库</li>
     *     <li>从执行中任务列表删除</li>
     *     <li>通知用户任务完成</li>
     * </ol>
     * 
     * @param messageBase 任务完成消息对象
     */
    private void handleExecutedMessage(MessageBase messageBase) {
        // 获取任务执行完成的消息数据
        HashMap<String, Object> data = messageBase.getData();
        
        // 【防御性编程】检查output字段是否存在
        HashMap<String, Object> output = (HashMap<String, Object>) data.get("output");
        if (output == null) {
            System.out.println(DateTime.now() + "\t\t⚠️ 收到异常的executed消息，output字段为null");
            return;
        }
        
        // 提取images数组（包含所有生成的图片元数据）
        List<HashMap<String, Object>> images = (List<HashMap<String, Object>>) output.get("images");
        if (images == null || images.isEmpty()) {
            System.out.println(DateTime.now() + "\t\t⚠️ 收到异常的executed消息，images字段为null或为空");
            return;
        }
        // 将图片元数据转换为可访问的URL列表
        // 格式：http://ComfyUI服务器地址/view?filename=xxx&type=xxx&subfolder=
        List<String> urls = images.stream().map((image) -> String.format("http://192.168.100.129:8188/view?filename=%s&type=%s&subfolder=", image.get("filename"), image.get("type")))
                .collect(Collectors.toList());
        // 构造发送给前端的消息对象
        HashMap<String, Object> temp = new HashMap<>();
        temp.put("type", "imageResult"); // 消息类型：图片生成结果
        temp.put("urls", urls); // 图片URL列表
        // 获取ComfyUI返回的任务ID
        if (data.get("prompt_id") == null) {
            System.out.println(DateTime.now() + "\t\t⚠️ 收到异常的executed消息，prompt_id为null");
            return;
        }
        String promptId = data.get("prompt_id").toString();
        // 根据promptId获取对应的任务信息
        ComfyuiTask task = redisService.getStartedTask(promptId);
        
        // 【关键Bug修复】无论任务是否存在，都必须释放信号量
        // 场景：任务执行超过60分钟，Redis中的任务数据已过期删除，但信号量已被占用
        // 如果不释放，会导致信号量永久泄露
        RSemaphore semaphore = redissonClient.getSemaphore(RunTaskJob.TASK_RUN_SEMAPHORE);
        semaphore.release();
        System.out.println(DateTime.now() + "\t\t任务完成，释放信号量，当前许可数: " + semaphore.availablePermits());
        
        // 如果任务不存在（可能已超时被清理），只释放信号量，不做其他操作
        if (task == null) {
            System.out.println(DateTime.now() + "\t\t⚠️ 收到完成消息，但任务已不存在（可能超时过期）: " + promptId);
            return;
        }
        
        // 【重要Bug修复】任务成功完成，从冻结账户扣除积分
        // 之前只是冻结积分，现在需要正式扣除
        userFundRecordService.pointsDeduction(task.getUserId(), task.getSize());
        System.out.println(DateTime.now() + "\t\t扣除用户" + task.getUserId() + "的积分: " + task.getSize());
        
        // 将生成的图片URL保存到数据库（用户历史记录）
        userResultService.saveList(urls,task.getUserId());
        // 从正在执行的任务集合中删除已完成的任务
        redisService.removeStartedTask(promptId);
        // 通过WebSocket向用户推送图片生成结果
        wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(temp));
    }

    /**
     * 处理任务执行进度消息
     * 
     * <p>ComfyUI在执行任务时会持续推送进度消息，包含当前步数和总步数
     * 
     * @param messageBase 进度消息对象，包含当前执行进度信息
     */
    private void handleProgressMessage(MessageBase messageBase) {
        /**
         * 消息格式示例：
         * messageBase
         * {
         *     "type": "progress",
         *     "data": {
         *         "value": 5,          // 当前已完成的步数
         *         "max": 20,           // 总步数
         *         "prompt_id": "594ac476-e599-47c1-a99f-bf8a384cfcdb",  // ComfyUI任务ID
         *         "node": "4"          // 当前执行的节点ID
         *     }
         * }
         */
        // 获取消息数据部分
        HashMap<String, Object> data = messageBase.getData();
        if (data == null || data.get("prompt_id") == null) {
            return;
        }
        // 从数据中提取ComfyUI任务ID（promptId）
        String promptId = data.get("prompt_id").toString();
        // 根据promptId从Redis获取对应的任务信息
        ComfyuiTask task = redisService.getStartedTask(promptId);
        // 设置消息类型为progress，确保前端能正确识别
        data.put("type", "progress");
        // 如果任务不存在（可能已过期被清理），直接返回不处理
        if (task == null) {
            return;
        }
        // 通过WebSocket向用户推送实时进度消息（让前端显示进度条）
        wsNoticeService.sendToUser(task.getWsClientId(), JSON.toJSONString(data));
    }
}
