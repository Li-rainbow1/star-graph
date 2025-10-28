package cn.itcast.star.graph.core.service;

import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiTask;

/**
 * Redis服务接口
 * 
 * <p>封装Redis操作，主要用于任务队列管理：
 * <ul>
 *     <li>使用Redis的ScoredSortedSet实现优先级队列</li>
 *     <li>使用Redis的String存储任务详情</li>
 *     <li>跟踪正在执行的任务</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
public interface RedisService {
    /**
     * 检查队列中是否有任务
     * 
     * @return true-有任务，false-无任务
     */
    boolean hasQueueTask();

    /**
     * 添加任务到队列
     * 
     * <p>使用当前时间戳作为分数，分数越低优先级越高
     * 
     * @param comfyuiTask 要添加的任务
     * @return 添加后的任务对象（包含队列位置信息）
     */
    public ComfyuiTask addQueueTask(ComfyuiTask comfyuiTask);

    /**
     * 从队列中弹出一个任务
     * 
     * <p>取出分数最小（优先级最高）的任务
     * 
     * @return 队列中优先级最高的任务，无任务时返回null
     */
    public ComfyuiTask popQueueTask();

    /**
     * 添加已开始执行的任务
     * 
     * <p>用于跟踪正在ComfyUI中执行的任务
     * 
     * @param promptId ComfyUI任务ID
     * @param task 任务对象
     */
    void addStartedTask(String promptId, ComfyuiTask task);

    /**
     * 获取正在执行的任务
     * 
     * @param promptId ComfyUI任务ID
     * @return 任务对象
     */
    ComfyuiTask getStartedTask(String promptId);

    /**
     * 从队列中获取任务详情
     * 
     * @param taskId 任务ID
     * @return 任务对象
     */
    ComfyuiTask getQueueTask(String taskId);

    /**
     * 从队列中移除任务
     * 
     * @param taskId 任务ID
     * @return true-移除成功，false-任务不存在
     */
    boolean removeQueueTask(String taskId);

    /**
     * 提升任务优先级
     * 
     * <p>减小任务的分数来提升优先级
     * 
     * @param taskId 任务ID
     * @param increment 要减小的分数值（传负数）
     * @return true-操作成功，false-任务不存在
     */
    boolean increasePriority(String taskId, double increment);

    /**
     * 获取任务在队列中的排名位置
     * 
     * @param taskId 任务ID
     * @return 排名位置（0表示第一名）
     */
    Long getTaskRank(String taskId);

    /**
     * 删除正在执行的任务
     * 
     * <p>当任务执行完成或失败时，需要从正在执行的任务集合中删除
     * 
     * @param promptId ComfyUI任务ID
     */
    void removeStartedTask(String promptId);
}
