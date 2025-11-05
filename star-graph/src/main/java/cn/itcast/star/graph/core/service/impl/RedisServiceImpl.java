package cn.itcast.star.graph.core.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiTask;
import cn.itcast.star.graph.core.service.RedisService;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis服务实现 - 使用ZSet实现任务优先级队列
 */
@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    
    /** 等待队列任务详情Key前缀 */
    private final static String TASK_KEY_PREFIX = "task_";
    /** 分布式自增ID Key */
    private final static String DISTRIBUTED_ID_KEY = "DISTRIBUTED_ID";
    /** 分布式优先级队列 Key (ZSet) */
    private final static String DISTRIBUTED_QUEUE_KEY = "DISTRIBUTED_QUEUE";
    /** 正在执行任务Key前缀 */
    private final static String RUN_TASK_KEY = "run_task_";

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    
    /**
     * 检查队列中是否有待处理任务
     */
    @Override
    public boolean hasQueueTask() {
        // 快速判断队列是否有任务
        return stringRedisTemplate.opsForZSet().size(DISTRIBUTED_QUEUE_KEY) > 0;
    }

    /**
     * 添加任务到优先级队列
     * 
     * @param comfyuiTask 任务对象
     * @return 包含队列序号的任务对象
     */
    @Override
    public ComfyuiTask addQueueTask(ComfyuiTask comfyuiTask) {
        // 生成分布式自增ID作为优先级分值（score越小优先级越高）
        Long score = stringRedisTemplate.opsForValue().increment(DISTRIBUTED_ID_KEY);
        // 添加到ZSet优先级队列
        stringRedisTemplate.opsForZSet().add(DISTRIBUTED_QUEUE_KEY, comfyuiTask.getId(), score);

        // 计算任务序号 = 正在执行的任务数 + 队列排名 + 1
        Long runningCount = getRunningTaskCount();
        Long rank = stringRedisTemplate.opsForZSet().rank(DISTRIBUTED_QUEUE_KEY, comfyuiTask.getId());
        comfyuiTask.setIndex(runningCount + (rank != null ? rank + 1 : 1));

        // 保存任务详情
        stringRedisTemplate.opsForValue().set(TASK_KEY_PREFIX + comfyuiTask.getId(), JSON.toJSONString(comfyuiTask));
        return comfyuiTask;
    }

    /**
     * 从队列中弹出优先级最高的任务
     * 
     * <p>使用ZPOPMIN原子操作弹出score最小的任务
     * 
     * @return 优先级最高的任务对象，队列为空时返回null
     */
    @Override
    public ComfyuiTask popQueueTask() {
        Long size = stringRedisTemplate.opsForZSet().size(DISTRIBUTED_QUEUE_KEY);
        if (size > 0) {
            // 弹出优先级最高的任务（原子操作）
            ZSetOperations.TypedTuple<String> task = stringRedisTemplate.opsForZSet().popMin(DISTRIBUTED_QUEUE_KEY);
            if (task != null && task.getValue() != null) {
                String taskId = task.getValue();
                String json = stringRedisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
                stringRedisTemplate.delete(TASK_KEY_PREFIX + taskId);
                
                if (StrUtil.isNotEmpty(json)) {
                    ComfyuiTask comfyuiTask = JSON.parseObject(json, ComfyuiTask.class);
                    comfyuiTask.setIndex(1);
                    
                    // 创建临时占位符，保证执行数统计准确
                    stringRedisTemplate.opsForValue().set(
                        RUN_TASK_KEY + "temp_" + taskId, 
                        JSON.toJSONString(comfyuiTask), 
                        Duration.ofMinutes(10)
                    );
                    return comfyuiTask;
                }
                // 解析失败则跳过
            }
        }
        return null;
    }

    /**
     * 添加已开始执行的任务到Redis
     */
    @Override
    public void addStartedTask(String promptId, ComfyuiTask task) {
        // 将序号更新为1
        task.setIndex(1);
        
        // 先创建正式任务，确保任务始终可被查找到（避免取消任务时找不到）
        stringRedisTemplate.opsForValue().set(RUN_TASK_KEY + promptId, JSON.toJSONString(task), Duration.ofMinutes(60));
        
        // 再删除临时占位符，避免短暂的重复统计
        if (task.getId() != null) {
            String tempKey = RUN_TASK_KEY + "temp_" + task.getId();
            Boolean deleted = stringRedisTemplate.delete(tempKey);
            
            if (Boolean.FALSE.equals(deleted)) {
                // 临时占位符删除失败，检查是否真的存在
                String tempValue = stringRedisTemplate.opsForValue().get(tempKey);
                if (tempValue != null) {
                    log.error("临时占位符存在但无法删除: taskId={}, tempKey={}", task.getId(), tempKey);
                }
            }
        } else {
            log.error("任务ID为null，无法删除临时占位符: promptId={}", promptId);
        }
    }

    /**
     * 根据promptId获取正在执行的任务详情
     */
    @Override
    public ComfyuiTask getStartedTask(String promptId) {
        // 根据ComfyUI返回的promptId获取正在执行的任务详情
        String json = stringRedisTemplate.opsForValue().get(RUN_TASK_KEY + promptId);
        // 如果JSON不为空，反序列化为任务对象
        if (StrUtil.isNotEmpty(json)) {
            return JSON.parseObject(json, ComfyuiTask.class);
        }
        // 任务不存在或已过期，返回null
        return null;
    }

    /**
     * 根据taskId获取等待队列中的任务详情
     */
    @Override
    public ComfyuiTask getQueueTask(String taskId) {
        // 根据任务ID从Redis获取队列中的任务详情
        String json = stringRedisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
        // 如果JSON不为空，反序列化为任务对象
        if (StrUtil.isNotEmpty(json)) {
            return JSON.parseObject(json, ComfyuiTask.class);
        }
        // 任务不存在（可能已被弹出或删除），返回null
        return null;
    }

    /**
     * 从队列中移除指定任务
     */
    @Override
    public boolean removeQueueTask(String taskId) {
        // 从ZSet优先级队列中删除指定任务ID，返回删除的元素数量
        Long removed = stringRedisTemplate.opsForZSet().remove(DISTRIBUTED_QUEUE_KEY, taskId);
        // 删除对应的任务详情数据，返回是否删除成功
        Boolean deleted = stringRedisTemplate.delete(TASK_KEY_PREFIX + taskId);
        // 只有队列和详情都删除成功才返回true
        return removed != null && removed > 0 && deleted != null && deleted;
    }

    /**
     * 提升任务优先级（插队功能）
     */
    @Override
    public boolean increasePriority(String taskId, double increment) {
        // ZSet按score升序；传入正数，这里取负以提升优先级
        Double newScore = stringRedisTemplate.opsForZSet().incrementScore(DISTRIBUTED_QUEUE_KEY, taskId, -increment);
        // incrementScore成功返回新分值，失败返回null
        return newScore != null;
    }

    /**
     * 获取任务的实时排队序号
     * 正在执行返回1，等待队列返回实际序号，已完成返回null
     */
    @Override
    public Long getTaskRank(String taskId) {
        // 获取任务在队列中的排名（从0开始）
        Long rank = stringRedisTemplate.opsForZSet().rank(DISTRIBUTED_QUEUE_KEY, taskId);
        
        // 如果任务不在队列中，检查是否在正在执行
        if (rank == null) {
            // 1. 检查临时占位符（任务刚从队列弹出，还未提交到ComfyUI）
            String tempTask = stringRedisTemplate.opsForValue().get(RUN_TASK_KEY + "temp_" + taskId);
            if (tempTask != null) {
                // 任务正在执行中，返回序号1
                return 1L;
            }
            
            // 2. 检查是否已提交到ComfyUI执行（遍历所有run_task_*，匹配taskId）
            java.util.Set<String> runningKeys = stringRedisTemplate.keys(RUN_TASK_KEY + "*");
            if (runningKeys != null) {
                for (String key : runningKeys) {
                    // 跳过临时占位符key（已经检查过了）
                    if (key.contains("temp_")) {
                        continue;
                    }
                    String json = stringRedisTemplate.opsForValue().get(key);
                    if (StrUtil.isNotEmpty(json)) {
                        try {
                            ComfyuiTask task = JSON.parseObject(json, ComfyuiTask.class);
                            if (task != null && taskId.equals(task.getId())) {
                                // 找到正在执行的任务，返回序号1
                                return 1L;
                            }
                        } catch (Exception e) {
                            // 忽略JSON解析异常，继续检查下一个
                        }
                    }
                }
            }
            
            // 任务既不在队列也不在执行中，返回null（已完成或被取消）
            return null;
        }
        
        // 任务在队列中，计算序号
        // 序号 = 正在执行的任务数 + 队列排名 + 1
        // 这样可以确保序号是连续的，告诉用户前面还有多少任务（包括正在执行的）
        // 例如：1个任务执行中，队列第一个任务rank=0，序号=1+0+1=2
        Long runningCount = getRunningTaskCount();
        return runningCount + rank + 1;
    }

    /**
     * 删除正在执行的任务记录
     */
    @Override
    public void removeStartedTask(String promptId) {
        // 删除正在执行的任务详情数据
        // 删除后run_task_*前缀的key数量会自动减少，无需额外维护
        stringRedisTemplate.delete(RUN_TASK_KEY + promptId);
    }

    /**
     * 获取正在执行的任务数量
     */
    private Long getRunningTaskCount() {
        java.util.Set<String> keys = stringRedisTemplate.keys(RUN_TASK_KEY + "*");
        return keys != null ? (long) keys.size() : 0L;
    }
}
