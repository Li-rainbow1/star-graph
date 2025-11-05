package cn.itcast.star.graph.core.job;

import cn.itcast.star.graph.core.service.UserFundRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 退款补偿定时任务
 * 
 * <p>自动处理因系统异常导致的退款失败，定期重试直到成功
 * <ul>
 *     <li>每5分钟扫描一次补偿队列</li>
 *     <li>每条记录最多重试10次</li>
 *     <li>重试成功后删除记录</li>
 *     <li>重试失败记录日志，人工介入</li>
 * </ul>
 */
@Slf4j
@Component
public class RefundCompensationJob {
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private UserFundRecordService userFundRecordService;
    
    private static final int MAX_RETRY_COUNT = 10;  // 最大重试次数
    private static final String COMPENSATION_PATTERN = "refund_compensation:*";
    
    /**
     * 每5分钟执行一次补偿任务（上次执行完成后延迟5分钟再执行）
     * 
     * <p>使用fixedDelay而非fixedRate的原因：
     * <ul>
     *     <li>避免任务堆积：如果处理时间较长，不会立即开始下次执行</li>
     *     <li>避免并发问题：确保同一时间只有一个补偿任务在运行</li>
     *     <li>适合处理时间不固定的场景：补偿记录数量可能波动</li>
     * </ul>
     */
    @Scheduled(fixedDelay = 300000)  // 上次执行完成后延迟5分钟
    public void processRefundCompensation() {
        log.info("开始执行退款补偿任务");
        
        try {
            // 查找所有待补偿的记录
            Set<String> compensationKeys = stringRedisTemplate.keys(COMPENSATION_PATTERN);
            
            if (compensationKeys == null || compensationKeys.isEmpty()) {
                log.debug("当前没有需要补偿的退款记录");
                return;
            }
            
            log.info("发现{}条待补偿的退款记录", compensationKeys.size());
            
            int successCount = 0;
            int failedCount = 0;
            int maxRetriedCount = 0;
            
            for (String key : compensationKeys) {
                try {
                    boolean processed = processOneCompensation(key);
                    if (processed) {
                        successCount++;
                    } else {
                        // 检查是否达到最大重试次数
                        String retryCountStr = (String) stringRedisTemplate.opsForHash().get(key, "retryCount");
                        int retryCount = retryCountStr != null ? Integer.parseInt(retryCountStr) : 0;
                        
                        if (retryCount >= MAX_RETRY_COUNT) {
                            maxRetriedCount++;
                            log.error("【人工介入】退款补偿达到最大重试次数：{}", key);
                        } else {
                            failedCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("处理补偿记录异常，Key: {}", key, e);
                    failedCount++;
                }
            }
            
            log.info("退款补偿任务执行完成：成功{}条，失败{}条，需要人工介入{}条", 
                    successCount, failedCount, maxRetriedCount);
            
        } catch (Exception e) {
            log.error("执行退款补偿任务异常", e);
        }
    }
    
    /**
     * 处理单条补偿记录
     * 
     * @param key Redis中的补偿记录Key
     * @return 是否处理成功
     */
    private boolean processOneCompensation(String key) {
        try {
            // 获取补偿信息
            Map<Object, Object> compensation = stringRedisTemplate.opsForHash().entries(key);
            
            if (compensation.isEmpty()) {
                log.warn("补偿记录为空，删除Key: {}", key);
                stringRedisTemplate.delete(key);
                return true;
            }
            
            Long userId = Long.parseLong((String) compensation.get("userId"));
            int amount = Integer.parseInt((String) compensation.get("amount"));
            String taskId = (String) compensation.get("taskId");
            String reason = (String) compensation.get("reason");
            int retryCount = Integer.parseInt((String) compensation.getOrDefault("retryCount", "0"));
            
            // 检查是否超过最大重试次数
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("【需要人工介入】退款补偿已达最大重试次数：用户{}，金额{}，任务{}，原因：{}", 
                        userId, amount, taskId, reason);
                return false;
            }
            
            log.info("尝试处理退款补偿（第{}次）：用户{}，金额{}，任务{}", 
                    retryCount + 1, userId, amount, taskId);
            
            // 尝试退款
            try {
                userFundRecordService.freezeReturn(userId, amount);
                
                // 退款成功，删除补偿记录
                stringRedisTemplate.delete(key);
                log.info("退款补偿成功：用户{}，金额{}，任务{}，共重试{}次", 
                        userId, amount, taskId, retryCount + 1);
                return true;
                
            } catch (Exception refundException) {
                // 退款失败，增加重试次数
                stringRedisTemplate.opsForHash().put(key, "retryCount", String.valueOf(retryCount + 1));
                stringRedisTemplate.opsForHash().put(key, "lastRetryTime", String.valueOf(System.currentTimeMillis()));
                
                log.warn("退款补偿失败（第{}次）：用户{}，金额{}，任务{}，错误：{}", 
                        retryCount + 1, userId, amount, taskId, refundException.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            log.error("处理补偿记录异常，Key: {}", key, e);
            return false;
        }
    }
}
