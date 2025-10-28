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
 * Redis服务实现类
 * 
 * <p>负责管理文生图任务的分布式队列和任务状态，使用Redis的多种数据结构实现高性能任务调度。
 * 
 * <h3>Redis数据结构设计：</h3>
 * <ul>
 *     <li><b>DISTRIBUTED_ID_KEY</b>: String类型，存储分布式自增ID，用于生成任务优先级分值</li>
 *     <li><b>DISTRIBUTED_QUEUE_KEY</b>: ZSet类型，优先级队列，按score从小到大排序</li>
 *     <li><b>task_{taskId}</b>: String类型，存储等待队列中的任务详情JSON</li>
 *     <li><b>run_task_{promptId}</b>: String类型，存储正在执行的任务详情JSON</li>
 *     <li><b>run_task_temp_{taskId}</b>: String类型，临时占位符，解决任务状态转换的时序窗口问题</li>
 * </ul>
 * 
 * <h3>任务状态流转：</h3>
 * <pre>
 * 1. 创建任务  → addQueueTask()     → 任务进入等待队列（DISTRIBUTED_QUEUE + task_*）
 * 2. 弹出任务  → popQueueTask()     → 创建临时占位符（run_task_temp_*）
 * 3. 开始执行  → addStartedTask()   → 正式存储（run_task_{promptId}），删除临时占位符
 * 4. 执行完成  → removeStartedTask() → 删除正在执行的任务记录
 * </pre>
 * 
 * <h3>序号计算规则：</h3>
 * <ul>
 *     <li><b>正在执行的任务：</b>序号固定为1</li>
 *     <li><b>等待队列中的任务：</b>序号 = 正在执行的任务数 + 队列排名 + 1</li>
 *     <li>例如：1个任务执行中，队列有3个任务，则序号分别为：1(执行中), 2, 3, 4(队列中)</li>
 * </ul>
 * 
 * <h3>并发安全性：</h3>
 * <ul>
 *     <li>使用Redis的原子操作（INCR、ZADD、ZPOPMIN等）保证并发安全</li>
 *     <li>临时占位符解决任务状态转换时的时序窗口问题</li>
 *     <li>所有key都设置过期时间，防止内存泄漏</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 * @see RedisService
 * @see ComfyuiTask
 */
@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    
    // ==================== Redis Key常量定义 ====================
    
    /** 任务详情key前缀，格式：task_{taskId}，存储等待队列中的任务完整信息 */
    private final static String TASK_KEY_PREFIX = "task_";
    
    /** 分布式自增ID的key，用于生成任务的优先级分值（score），值越小优先级越高 */
    private final static String DISTRIBUTED_ID_KEY = "DISTRIBUTED_ID";
    
    /** 分布式优先级队列的key，使用ZSet数据结构，按score从小到大排序 */
    private final static String DISTRIBUTED_QUEUE_KEY = "DISTRIBUTED_QUEUE";
    
    /** 正在执行的任务key前缀，格式：run_task_{promptId} 或 run_task_temp_{taskId} */
    private final static String RUN_TASK_KEY = "run_task_";

    // ==================== 依赖注入 ====================
    
    /** Spring Redis模板，用于操作Redis的String、ZSet等数据结构 */
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    // ==================== 队列管理方法 ====================
    
    /**
     * 检查队列中是否有待处理任务
     * 
     * <p>通过查询ZSet的大小判断队列是否为空，时间复杂度O(1)。
     * 
     * @return true-队列中有任务，false-队列为空
     */
    @Override
    public boolean hasQueueTask() {
        // 获取Redis ZSet队列的元素数量，判断是否有待处理任务
        return stringRedisTemplate.opsForZSet().size(DISTRIBUTED_QUEUE_KEY) > 0;
    }

    /**
     * 添加任务到优先级队列
     * 
     * <p>实现步骤：
     * <ol>
     *     <li>获取分布式自增ID作为任务的优先级分值（score）</li>
     *     <li>将任务ID和分值添加到ZSet优先级队列</li>
     *     <li>计算任务在队列中的实际序号（考虑正在执行的任务）</li>
     *     <li>保存任务详情到Redis String</li>
     * </ol>
     * 
     * <p><b>序号计算公式：</b>序号 = 正在执行的任务数 + 队列排名 + 1
     * 
     * <p><b>并发安全：</b>使用Redis的INCR原子操作生成唯一分值，确保先提交的任务先执行
     * 
     * @param comfyuiTask 要添加的任务对象，必须包含唯一的taskId
     * @return 添加后的任务对象，包含计算好的队列序号（index字段）
     */
    @Override
    public ComfyuiTask addQueueTask(ComfyuiTask comfyuiTask) {
        // 使用Redis的INCR命令获取分布式自增ID作为任务的分值（score）
        // 分值越小优先级越高，自增ID确保先提交的任务先执行
        Long score = stringRedisTemplate.opsForValue().increment(DISTRIBUTED_ID_KEY);

        // 将任务ID作为member，分值作为score，添加到ZSet有序集合中
        // ZSet会自动按照score从小到大排序，实现优先级队列
        stringRedisTemplate.opsForZSet().add(DISTRIBUTED_QUEUE_KEY, comfyuiTask.getId(), score);

        // 获取正在执行的任务数量（通过扫描run_task_*前缀的key）
        Long runningCount = getRunningTaskCount();
        // 获取任务在等待队列中的排名（rank从0开始计数）
        Long rank = stringRedisTemplate.opsForZSet().rank(DISTRIBUTED_QUEUE_KEY, comfyuiTask.getId());
        // 设置任务序号 = 正在执行的任务数 + 队列排名 + 1
        // 这样可以确保即使有任务正在执行，新任务的序号也是连续的
        comfyuiTask.setIndex(runningCount + (rank != null ? rank + 1 : 1));

        // 将完整的任务对象序列化为JSON，使用String类型存储到Redis
        // key格式：task_{任务ID}，用于后续查询、取消等操作
        stringRedisTemplate.opsForValue().set(TASK_KEY_PREFIX + comfyuiTask.getId(), JSON.toJSONString(comfyuiTask));

        // 返回带有队列序号的任务对象
        return comfyuiTask;
    }

    /**
     * 从队列中弹出优先级最高的任务
     * 
     * <p>实现步骤：
     * <ol>
     *     <li>使用ZPOPMIN原子操作弹出ZSet中score最小（优先级最高）的任务ID</li>
     *     <li>根据任务ID获取并删除任务详情JSON</li>
     *     <li>立即创建临时占位符（run_task_temp_{taskId}），解决时序窗口问题</li>
     *     <li>返回任务对象供后续提交到ComfyUI</li>
     * </ol>
     * 
     * <p><b>时序窗口问题：</b>
     * 任务从队列弹出到提交ComfyUI之间有短暂时间差，此时任务不在队列也没有promptId。
     * 创建临时占位符确保getRunningTaskCount()和getTaskRank()能正确识别任务状态。
     * 
     * <p><b>并发安全：</b>ZPOPMIN是原子操作，多个线程同时弹出不会获取到相同任务
     * 
     * @return 优先级最高的任务对象，队列为空时返回null
     */
    @Override
    public ComfyuiTask popQueueTask() {
        // 获取队列中的任务数量
        Long size = stringRedisTemplate.opsForZSet().size(DISTRIBUTED_QUEUE_KEY);
        // 如果队列中有任务
        if (size > 0) {
            // 使用popMin原子操作：弹出并删除ZSet中分值最小（优先级最高）的任务
            // 返回的TypedTuple包含任务ID（value）和分值（score）
            ZSetOperations.TypedTuple<String> task = stringRedisTemplate.opsForZSet().popMin(DISTRIBUTED_QUEUE_KEY);
            // 检查弹出的任务是否为空
            if (task != null && task.getValue() != null) {
                // 获取任务ID
                String taskId = task.getValue();
                // 根据任务ID从Redis String中获取完整的任务详情JSON
                String json = stringRedisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
                // 立即删除任务详情，释放内存（任务已经弹出，不再需要保存）
                stringRedisTemplate.delete(TASK_KEY_PREFIX + taskId);
                // 检查JSON是否为空，防止数据不一致导致反序列化失败
                if (StrUtil.isNotEmpty(json)) {
                    // 将JSON反序列化为ComfyuiTask对象
                    ComfyuiTask comfyuiTask = JSON.parseObject(json, ComfyuiTask.class);
                    
                    // 任务从队列弹出时，将序号更新为1
                    comfyuiTask.setIndex(1);
                    
                    // 立即创建占位符，使用临时taskId作为key
                    // 这样可以确保getRunningTaskCount()统计准确，避免时序窗口问题
                    // 后续addStartedTask会用promptId替换这个占位符
                    stringRedisTemplate.opsForValue().set(
                        RUN_TASK_KEY + "temp_" + taskId, 
                        JSON.toJSONString(comfyuiTask), 
                        Duration.ofMinutes(10)  // 临时key，10分钟过期
                    );
                    return comfyuiTask;
                }
            }
        }
        // 队列为空或数据异常，返回null
        return null;
    }

    /**
     * 添加已开始执行的任务到Redis
     * 
     * <p>当任务成功提交到ComfyUI后调用此方法，执行以下操作：
     * <ol>
     *     <li>使用ComfyUI返回的promptId作为key，存储任务详情</li>
     *     <li>设置60分钟过期时间，防止长时间未完成的任务占用内存</li>
     *     <li>删除popQueueTask()时创建的临时占位符</li>
     * </ol>
     * 
     * <p><b>Key格式：</b>run_task_{promptId}
     * 
     * <p><b>过期策略：</b>60分钟后自动删除，正常情况下任务完成后会主动调用removeStartedTask()删除
     * 
     * @param promptId ComfyUI返回的任务唯一标识
     * @param task 任务对象，包含用户ID、WebSocket客户端ID等信息
     */
    @Override
    public void addStartedTask(String promptId, ComfyuiTask task) {
        // 先删除临时占位符，再创建正式任务，避免短暂的重复统计
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
        
        // 将序号更新为1
        task.setIndex(1);
        
        // 将已提交到ComfyUI执行的任务保存到Redis
        stringRedisTemplate.opsForValue().set(RUN_TASK_KEY + promptId, JSON.toJSONString(task), Duration.ofMinutes(60));
    }

    /**
     * 根据promptId获取正在执行的任务详情
     * 
     * <p>用于ComfyUI回调时根据promptId查找对应的任务信息，以便：
     * <ul>
     *     <li>获取WebSocket客户端ID，向用户推送进度和结果</li>
     *     <li>获取用户ID，保存生成的图片到用户历史记录</li>
     *     <li>获取图片数量，归还用户冻结的积分</li>
     * </ul>
     * 
     * @param promptId ComfyUI任务ID
     * @return 任务对象，任务不存在或已过期时返回null
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
     * 
     * <p>用于以下场景：
     * <ul>
     *     <li>用户取消任务时，验证任务所属权限</li>
     *     <li>用户插队时，验证任务是否仍在队列中</li>
     *     <li>查询任务排名时，验证任务权限</li>
     * </ul>
     * 
     * @param taskId 本地任务ID（提交任务时系统生成的UUID）
     * @return 任务对象，任务不存在（已弹出或被删除）时返回null
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
     * 
     * <p>用于用户主动取消任务，需要同时删除：
     * <ol>
     *     <li>ZSet队列中的任务ID（通过ZREM命令）</li>
     *     <li>String类型的任务详情数据（通过DEL命令）</li>
     * </ol>
     * 
     * <p><b>注意：</b>只能删除等待队列中的任务，已经开始执行的任务无法取消
     * 
     * @param taskId 要删除的任务ID
     * @return true-删除成功，false-任务不存在或删除失败
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
     * 
     * <p>通过减小任务的score值来提升优先级，因为ZSet按score从小到大排序。
     * 使用ZINCRBY命令原子性地修改score，传入负数实现减小。
     * 
     * <p><b>插队规则：</b>
     * <ul>
     *     <li>每次插队消耗5积分</li>
     *     <li>score减小10.0，相当于向前插10个位置</li>
     *     <li>已经排在第1位的任务无法再插队</li>
     *     <li>正在执行的任务无法插队</li>
     * </ul>
     * 
     * @param taskId 要插队的任务ID
     * @param increment 要减小的分值（传入正数，方法内部会取负）
     * @return true-插队成功，false-任务不存在
     */
    @Override
    public boolean increasePriority(String taskId, double increment) {
        // 减小分值可以提升优先级（ZSet 按分值从小到大排序）
        Double newScore = stringRedisTemplate.opsForZSet().incrementScore(DISTRIBUTED_QUEUE_KEY, taskId, -increment);
        // incrementScore成功返回新分值，失败返回null
        return newScore != null;
    }

    /**
     * 获取任务的实时排队序号
     * 
     * <p>序号计算规则：
     * <ul>
     *     <li><b>正在执行的任务：</b>返回1（通过临时占位符或promptId识别）</li>
     *     <li><b>等待队列中的任务：</b>返回 正在执行的任务数 + 队列排名 + 1</li>
     *     <li><b>已完成或被取消：</b>返回null</li>
     * </ul>
     * 
     * <p><b>实现逻辑：</b>
     * <ol>
     *     <li>先查询任务在ZSet队列中的排名（ZRANK命令）</li>
     *     <li>如果不在队列中，检查是否在正在执行的任务集合中</li>
     *     <li>首先检查临时占位符（run_task_temp_{taskId}）</li>
     *     <li>然后遍历所有正在执行的任务，匹配taskId</li>
     *     <li>如果找到则返回1，否则返回null（任务已完成）</li>
     *     <li>如果在队列中，获取正在执行的任务数，计算实际序号</li>
     * </ol>
     * 
     * <p><b>使用场景：</b>
     * <ul>
     *     <li>前端轮询查询任务排队进度</li>
     *     <li>插队前验证任务当前排名</li>
     *     <li>显示用户排队位置</li>
     * </ul>
     * 
     * @param taskId 本地任务ID
     * @return 任务序号（从1开始），任务不存在或已完成返回null
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
     * 
     * <p>在以下情况下调用：
     * <ul>
     *     <li>任务执行成功完成</li>
     *     <li>任务执行失败</li>
     *     <li>ComfyUI返回错误</li>
     * </ul>
     * 
     * <p>删除后，run_task_*前缀的key数量会自动减少，
     * getRunningTaskCount()统计的正在执行任务数会自动更新，
     * 等待队列中任务的序号会自动递减。
     * 
     * @param promptId ComfyUI任务ID或临时占位符（temp_{taskId}）
     */
    @Override
    public void removeStartedTask(String promptId) {
        // 删除正在执行的任务详情数据
        // 删除后run_task_*前缀的key数量会自动减少，无需额外维护
        stringRedisTemplate.delete(RUN_TASK_KEY + promptId);
    }

    /**
     * 获取正在执行的任务数量
     * 
     * <p>注意：
     * <ul>
     *     <li>keys命令会遍历所有key，但由于正在执行的任务数量通常很少（几个到几十个），对Redis性能影响可接受</li>
     *     <li>会同时统计临时占位符(run_task_temp_*)和正式任务(run_task_{promptId})，这是正确的</li>
     *     <li>keys命令返回的结果已经自动过滤掉了已过期的key</li>
     * </ul>
     * 
     * @return 正在执行的任务数量
     */
    private Long getRunningTaskCount() {
        java.util.Set<String> keys = stringRedisTemplate.keys(RUN_TASK_KEY + "*");
        return keys != null ? (long) keys.size() : 0L;
    }
}
