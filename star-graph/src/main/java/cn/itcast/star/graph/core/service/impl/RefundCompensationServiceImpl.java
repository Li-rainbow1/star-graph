package cn.itcast.star.graph.core.service.impl;

import cn.itcast.star.graph.core.service.RefundCompensationService;
import cn.itcast.star.graph.core.service.UserFundRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 退款补偿服务实现
 */
@Slf4j
@Service
public class RefundCompensationServiceImpl implements RefundCompensationService {
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private UserFundRecordService userFundRecordService;
    
    @Override
    public void recordCompensation(Long userId, int amount, String taskId, String reason) {
        try {
            String compensationKey = "refund_compensation:" + System.currentTimeMillis() + ":" + taskId;
            
            // 使用Hash存储补偿信息
            stringRedisTemplate.opsForHash().put(compensationKey, "userId", String.valueOf(userId));
            stringRedisTemplate.opsForHash().put(compensationKey, "amount", String.valueOf(amount));
            stringRedisTemplate.opsForHash().put(compensationKey, "taskId", taskId);
            stringRedisTemplate.opsForHash().put(compensationKey, "reason", reason);
            stringRedisTemplate.opsForHash().put(compensationKey, "createTime", String.valueOf(System.currentTimeMillis()));
            stringRedisTemplate.opsForHash().put(compensationKey, "retryCount", "0");
            
            // 设置过期时间：7天（足够时间让定时任务处理）
            stringRedisTemplate.expire(compensationKey, 7, TimeUnit.DAYS);
            
            log.info("已记录退款补偿：用户{}，金额{}，任务{}，补偿Key: {}", userId, amount, taskId, compensationKey);
        } catch (Exception e) {
            // 记录补偿信息失败，记录严重错误日志
            log.error("【严重错误】记录退款补偿失败！用户{}，金额{}，任务{}，需要人工介入", userId, amount, taskId, e);
        }
    }
    
    @Override
    public boolean safeRefund(Long userId, int amount, String taskId, String reason) {
        try {
            userFundRecordService.freezeReturn(userId, amount);
            log.info("退款成功：用户{}，金额{}，任务{}", userId, amount, taskId);
            return true;
        } catch (Exception refundException) {
            log.error("退款失败，已加入补偿队列：用户{}，金额{}，任务{}，原因：{}", 
                    userId, amount, taskId, reason, refundException);
            recordCompensation(userId, amount, taskId, reason);
            return false;
        }
    }
}
