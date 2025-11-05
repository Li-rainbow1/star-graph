package cn.itcast.star.graph.core.service;

/**
 * 退款补偿服务
 * 
 * <p>用于记录因系统异常导致的退款失败，供定时任务自动重试
 */
public interface RefundCompensationService {
    
    /**
     * 记录需要补偿的退款
     * 
     * @param userId 用户ID
     * @param amount 退款金额（积分数）
     * @param taskId 任务ID
     * @param reason 失败原因
     */
    void recordCompensation(Long userId, int amount, String taskId, String reason);
    
    /**
     * 安全退款（自动处理失败并记录补偿）
     * 
     * @param userId 用户ID
     * @param amount 退款金额（积分数）
     * @param taskId 任务ID
     * @param reason 失败原因标识
     * @return 是否退款成功
     */
    boolean safeRefund(Long userId, int amount, String taskId, String reason);
}
