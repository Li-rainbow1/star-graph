package cn.itcast.star.graph.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.itcast.star.graph.comfyui.client.api.ComfyuiApi;
import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiModel;
import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiRequestDto;
import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiTask;
import cn.itcast.star.graph.core.common.Constants;
import cn.itcast.star.graph.core.dto.common.PageResult;
import cn.itcast.star.graph.core.dto.request.Text2ImageCancelReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImageListReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImagePriorityReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImageReqDto;
import cn.itcast.star.graph.core.dto.respone.Text2ImageResDto;
import cn.itcast.star.graph.core.exception.CustomException;
import cn.itcast.star.graph.core.pojo.UserResult;
import cn.itcast.star.graph.core.service.*;
import cn.itcast.star.graph.core.utils.UserUtils;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文生图服务实现 - 处理任务创建、取消、插队等核心业务
 */
@Slf4j
@Service
public class Text2ImageServiceImpl implements Text2ImageService {
    
    @Autowired
    OllamaService ollamaService;
    
    @Autowired
    FreemarkerService freemarkerService;
    
    @Autowired
    RedisService redisService;
    
    @Autowired
    UserFundRecordService userFundRecordService;
    
    @Autowired
    UserResultService userResultService;
    
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    ComfyuiApi comfyuiApi;
    
    @Autowired
    RefundCompensationService refundCompensationService;
    
    private static final String LOCK_KEY_PREFIX = "lock:task:";
    private static final long LOCK_TIMEOUT = 10;
    private static final int PRIORITY_COST = 5;
    private static final double PRIORITY_INCREMENT = 10.0;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int INTERRUPT_MAX_RETRIES = 3;  // 中断接口最大重试次数
    private static final long INTERRUPT_RETRY_DELAY_MS = 500;  // 重试间隔(毫秒)

    /**
     * 把请求参数封装成comfyui的请求对象
     */
    public ComfyuiTask getComfyuiTask(Text2ImageReqDto text2ImageReqDto) throws Exception {
        // 复制基础参数（将请求DTO映射到模型对象）
        ComfyuiModel comfyuiModel = new ComfyuiModel();
        BeanUtil.copyProperties(text2ImageReqDto, comfyuiModel, true);
        comfyuiModel.setModelName(text2ImageReqDto.modelName());
        comfyuiModel.setSamplerName(text2ImageReqDto.samplerName());
        comfyuiModel.setScheduler(text2ImageReqDto.scheduler());
        comfyuiModel.setWidth(text2ImageReqDto.width());
        comfyuiModel.setHeight(text2ImageReqDto.height());

        // 处理提示词：添加画质增强前缀并翻译（避免中英混合导致模型理解偏差）
        comfyuiModel.setPropmt("(8k, best quality, masterpiece),(high detailed skin)," + ollamaService.translate(text2ImageReqDto.getPropmt()));
        // 处理负面提示词：翻译并添加常见负面关键词（降低坏脸/多指等概率）
        comfyuiModel.setReverse(ollamaService.translate(text2ImageReqDto.getReverse()) + ",bad face,naked,bad finger,bad arm,bad leg,bad eye");

        // 使用Freemarker生成ComfyUI工作流JSON（模板化工作流便于统一维护）
        String prompt = freemarkerService.renderText2Image(comfyuiModel);
        ComfyuiRequestDto comfyuiRequestDto = new ComfyuiRequestDto(Constants.COMFYUI_CLIENT_ID, JSON.parseObject(prompt));

        // 封装任务对象：包含WS客户端ID、请求体、用户与图片数量等
        ComfyuiTask comfyuiTask = new ComfyuiTask(text2ImageReqDto.getClientId(), comfyuiRequestDto);
        comfyuiTask.setUserId(UserUtils.getUser().getId());
        comfyuiTask.setSize(text2ImageReqDto.getSize());
        return comfyuiTask;
    }

    /**
     * 文生图接口实现
     */
    @Override
    public Text2ImageResDto textToImage(Text2ImageReqDto text2ImageReqDto) throws Exception {
        if (text2ImageReqDto.getSize() < 1) {
            throw new CustomException("请求参数错误！");
        }
        
        Long userId = UserUtils.getUser().getId();
        // 先冻结积分：任务完成时扣除；失败/异常时归还
        userFundRecordService.pointsFreeze(userId, text2ImageReqDto.getSize());
        
        try {
            // 组装任务并入队到Redis优先级队列
            ComfyuiTask comfyuiTask = getComfyuiTask(text2ImageReqDto);
            comfyuiTask = redisService.addQueueTask(comfyuiTask);
            Text2ImageResDto text2ImageResDto = new Text2ImageResDto();
            text2ImageResDto.setPid(comfyuiTask.getId());
            text2ImageResDto.setQueueIndex(comfyuiTask.getIndex());
            return text2ImageResDto;
        } catch (Exception e) {
            log.error("创建文生图任务失败，归还用户{}的积分{}", userId, text2ImageReqDto.getSize(), e);
            // 入队失败或系统异常：归还冻结积分（失败会自动补偿，无需阻塞用户）
            refundCompensationService.safeRefund(userId, text2ImageReqDto.getSize(), 
                    "create_task_" + System.currentTimeMillis(), "create_task_failed_refund");
            // 抛出原始异常，告知用户任务创建失败（退款会在后台处理）
            throw e;
        }
    }

    /**
     * 取消文生图任务（智能取消：队列中的直接删除，执行中的调用中断接口）
     *
     * @param cancelReqDto 取消请求参数
     * @throws Exception 处理异常
     */
    @Override
    public void cancelTask(Text2ImageCancelReqDto cancelReqDto) throws Exception {
        String tempId = cancelReqDto.getTempId();
        if (tempId == null || tempId.trim().isEmpty()) {
            throw new CustomException("任务ID不能为空");
        }
        
        Long currentUserId = UserUtils.getUser().getId();
        log.info("用户{}尝试取消任务{}", currentUserId, tempId);
        
        // 使用分布式锁防止并发取消
        String lockKey = LOCK_KEY_PREFIX + tempId;
        String lockValue = UUID.randomUUID().toString();
        
        if (!tryLock(lockKey, lockValue)) {
            log.warn("用户{}取消任务{}失败: 获取锁失败", currentUserId, tempId);
            throw new CustomException("操作过于频繁，请稍后再试");
        }
        
        try {
            // 检查任务状态：getTaskRank返回1表示正在执行，>1表示在队列中，null表示不存在
            Long currentRank = redisService.getTaskRank(tempId);
            log.info("用户{}取消任务{}，当前排名: {}", currentUserId, tempId, currentRank);
            
            if (currentRank == null) {
                // 任务不存在或已完成
                log.warn("用户{}取消任务{}失败: 任务不存在或已完成", currentUserId, tempId);
                throw new CustomException("任务不存在或已完成");
            }
            
            if (currentRank == 1L) {
                // 任务正在执行中，需要调用中断接口
                log.info("任务{}正在执行中（排名=1），用户{}尝试中断任务", tempId, currentUserId);
                
                ComfyuiTask runningTask = findRunningTask(tempId);
                if (runningTask == null) {
                    log.error("任务{}排名为1但找不到执行中的任务详情", tempId);
                    throw new CustomException("任务状态异常，请稍后重试");
                }
                
                // 验证任务所有权
                if (!currentUserId.equals(runningTask.getUserId())) {
                    log.warn("用户{}尝试取消非本人任务{}，任务所有者: {}", currentUserId, tempId, runningTask.getUserId());
                    throw new CustomException("无权限操作该任务");
                }
                
                // 第一步：调用中断接口（带重试机制）
                boolean interruptSuccess = interruptTaskWithRetry(tempId, currentUserId);
                
                if (!interruptSuccess) {
                    // 中断失败（重试多次后仍失败），不退款
                    log.error("用户{}中断任务{}失败（已重试{}次），任务将继续执行，不退款", 
                            currentUserId, tempId, INTERRUPT_MAX_RETRIES);
                    throw new CustomException("任务中断失败，请稍后重试");
                }
                
                // 第二步：中断成功后再退款
                log.info("用户{}成功中断任务{}，开始归还积分", currentUserId, tempId);
                boolean refundSuccess = refundCompensationService.safeRefund(
                        runningTask.getUserId(), runningTask.getSize(), tempId, "interrupt_refund_failed");
                
                if (!refundSuccess) {
                    // 退款失败，已自动加入补偿队列
                    throw new CustomException("任务已中断，积分正在处理中，请稍后查看账户余额");
                }
                return;
            }
            
            // 任务在队列中（排名>1），直接删除并退款
            log.info("任务{}在队列中（排名={}），直接删除", tempId, currentRank);
            ComfyuiTask queueTask = redisService.getQueueTask(tempId);
            if (queueTask == null) {
                log.error("任务{}有排名但获取详情失败", tempId);
                throw new CustomException("任务状态异常，请稍后重试");
            }
            
            // 验证权限
            if (!currentUserId.equals(queueTask.getUserId())) {
                throw new CustomException("无权限操作该任务");
            }
            
            // 从队列中删除任务
            boolean removed = redisService.removeQueueTask(tempId);
            if (!removed) {
                log.error("用户{}取消任务{}失败: Redis删除失败", currentUserId, tempId);
                throw new CustomException("任务取消失败");
            }
            
            // 归还冻结的积分（取消不扣费）
            boolean refundSuccess = refundCompensationService.safeRefund(
                    queueTask.getUserId(), queueTask.getSize(), tempId, "queue_cancel_refund_failed");
            
            if (!refundSuccess) {
                // 退款失败，已自动加入补偿队列
                throw new CustomException("任务已取消，积分正在处理中，请稍后查看账户余额");
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户{}取消任务{}发生系统异常", currentUserId, tempId, e);
            throw new CustomException("任务取消失败");
        } finally {
            try {
                // 释放分布式锁（使用Lua脚本保证原子性）
                unlock(lockKey, lockValue);
            } catch (Exception e) {
                log.error("释放锁失败: lockKey={}", lockKey, e);
            }
        }
    }

    /**
     * 尝试获取分布式锁
     * 
     * @param lockKey 锁的key
     * @param lockValue 锁的值（用于释放时验证）
     * @return 是否成功获取锁
     */
    private boolean tryLock(String lockKey, String lockValue) {
        // 使用Redis的SETNX命令（setIfAbsent）实现分布式锁
        // 只有当key不存在时才设置成功，同时设置过期时间防止死锁
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT, TimeUnit.SECONDS);
        // 返回是否成功获取锁（Boolean.TRUE.equals避免null异常）
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放分布式锁（使用Lua脚本保证原子性）
     * 
     * @param lockKey 锁的key
     * @param lockValue 锁的值（验证是否是自己的锁）
     */
    private void unlock(String lockKey, String lockValue) {
        // 使用Lua脚本释放锁，保证原子性（获取和删除是一个原子操作）
        // Lua脚本逻辑：先获取锁的值，如果匹配当前请求的lockValue，才删除
        // 这样可以防止误删其他请求的锁（例如锁过期后被其他请求获取）
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        // 创建Redis脚本对象，指定返回类型为Long
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        // 执行Lua脚本：KEYS[1]=lockKey，ARGV[1]=lockValue
        stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);
    }

    /**
     * 获取用户文生图历史列表
     *
     * @param listReqDto 请求分页参数
     * @return 分页结果
     */
    @Override
    public PageResult<List<UserResult>> getUserImageList(Text2ImageListReqDto listReqDto) {
        // 从ThreadLocal中获取当前登录用户ID
        Long userId = UserUtils.getUser().getId();

        // 参数校验：获取请求的页码和每页数量
        Integer pageNum = listReqDto.getPageNum();
        Integer pageSize = listReqDto.getPageSize();
        // 如果页码小于最小值，设置为最小值（1）
        if (pageNum < MIN_PAGE_SIZE) {
            pageNum = MIN_PAGE_SIZE;
        }
        // 如果每页数量超出合理范围，设置为默认值（10）
        if (pageSize < MIN_PAGE_SIZE || pageSize > MAX_PAGE_SIZE) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        // 构建 MyBatis Plus 的分页对象
        Page<UserResult> page = new Page<>(pageNum, pageSize);
        // 构建 Lambda 查询条件包装器
        LambdaQueryWrapper<UserResult> queryWrapper = new LambdaQueryWrapper<>();
        // 设置查询条件：只查询当前用户的记录，按创建时间倒序
        queryWrapper.eq(UserResult::getUserId, userId)
                .orderByDesc(UserResult::getCreatedTime);

        // 执行分页查询
        IPage<UserResult> resultPage = userResultService.page(page, queryWrapper);

        // 转换为自定义分页结果返回
        return PageResult.ok(resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * 提升任务优先级（插队）（使用分布式锁保证并发安全）
     *
     * @param priorityReqDto
     * @return 新的队列序号
     * @throws Exception
     */
    @Override
    public Long increasePriority(Text2ImagePriorityReqDto priorityReqDto) throws Exception {
        // 从请求DTO中获取任务临时ID
        String tempId = priorityReqDto.getTempId();
        // 校验任务ID不能为空
        if (tempId == null || tempId.trim().isEmpty()) {
            throw new CustomException("任务ID不能为空");
        }
        
        // 从ThreadLocal中获取当前登录用户ID（只获取一次，避免重复调用）
        Long currentUserId = UserUtils.getUser().getId();
        // 记录插队操作日志
        log.info("用户{}尝试为任务{}插队", currentUserId, tempId);
        
        // 构造分布式锁的key（每个任务一个锁）
        String lockKey = LOCK_KEY_PREFIX + tempId;
        // 生成随机锁值，用于释放时验证是否是自己的锁
        String lockValue = UUID.randomUUID().toString();
        
        // 尝试获取分布式锁，防止并发操作同一任务
        if (!tryLock(lockKey, lockValue)) {
            // 获取锁失败，说明有其他请求正在处理该任务
            log.warn("用户{}插队任务{}失败: 获取锁失败", currentUserId, tempId);
            throw new CustomException("操作过于频繁，请稍后再试");
        }
        
        try {
            // 先获取任务在队列中的纯排名（不包含正在执行的任务数），用于判断是否已经是第一名
            Long queueRank = stringRedisTemplate.opsForZSet().rank("DISTRIBUTED_QUEUE", tempId);
            
            // 检查任务是否在队列中
            if (queueRank == null) {
                // 任务不在队列中，可能已经开始执行或不存在
                log.warn("用户{}插队失败: 任务{}不在队列中", currentUserId, tempId);
                throw new CustomException("任务已经开始或不存在");
            }
            
            // 检查任务在队列中是否已经是第一名（rank从0开始，0表示第一名）
            if (queueRank == 0) {
                // 已经是队列第一名，无需插队
                log.info("用户{}插队失败: 任务{}已经是队列第一名", currentUserId, tempId);
                throw new CustomException("当前任务已经是第一名，无需插队");
            }
            
            // 验证任务权限（直接获取任务对象并检查权限，不重复查询rank）
            ComfyuiTask queueTask = redisService.getQueueTask(tempId);
            if (queueTask == null) {
                log.warn("用户{}插队失败: 任务{}详情不存在", currentUserId, tempId);
                throw new CustomException("任务不存在");
            }
            
            // 检查权限
            if (!currentUserId.equals(queueTask.getUserId())) {
                throw new CustomException("无权限操作该任务");
            }
            
            // 先扣除积分，再提升优先级，避免用户未付费但优先级已提升（资金一致性优先）
            userFundRecordService.directDeduction(queueTask.getUserId(), PRIORITY_COST);
            
            // 标记积分是否已扣除，用于异常时判断是否需要归还
            boolean pointsDeducted = true;
            
            try {
                // 再提升优先级（Redis操作，通过减小ZSet score来提升排名）
                // 积分已扣除，即使Redis操作失败也不会导致数据不一致
                boolean success = redisService.increasePriority(tempId, PRIORITY_INCREMENT);
                if (!success) {
                    // Redis操作失败，需要归还已扣除的积分
                    log.error("用户{}插队任务{}失败: Redis提升优先级失败，归还积分{}", currentUserId, tempId, PRIORITY_COST);
                    // 归还积分到可用账户
                    userFundRecordService.directRefund(queueTask.getUserId(), PRIORITY_COST);
                    throw new CustomException("提升优先级失败，积分已退还");
                }
                
                // 获取提升优先级后的新排名
                Long newRank = redisService.getTaskRank(tempId);
                // 记录插队成功日志，包含排名变化和消耗积分
                log.info("用户{}插队成功: 任务{}从队列第{}名提升到第{}名，消耗积分{}", 
                        currentUserId, tempId, queueRank + 1, newRank, PRIORITY_COST);
                // 返回新的排名位置（如果查询失败默认返回1）
                return newRank != null ? newRank : 1L;
            } catch (CustomException e) {
                // 业务异常（如：Redis操作失败等）直接抛出，积分已在上面归还
                throw e;
            } catch (Exception e) {
                // 系统异常时，需要归还已扣除的积分
                if (pointsDeducted) {
                    log.error("用户{}插队任务{}发生系统异常，归还积分{}", currentUserId, tempId, PRIORITY_COST, e);
                    try {
                        userFundRecordService.directRefund(queueTask.getUserId(), PRIORITY_COST);
                    } catch (Exception refundException) {
                        // 归还积分失败，记录严重错误，需要人工介入
                        log.error("归还积分失败！用户{}需要人工补偿积分{}", queueTask.getUserId(), PRIORITY_COST, refundException);
                    }
                }
                throw new CustomException("操作失败，积分已退还");
            }
        } catch (CustomException e) {
            // 业务异常（如：已是第一名、积分不足等）直接抛出，不记录堆栈
            throw e;
        } catch (Exception e) {
            // 未扣除积分前的异常，直接抛出
            log.error("用户{}插队任务{}发生异常", currentUserId, tempId, e);
            throw e;
        } finally {
            // finally块确保锁一定会被释放，即使发生异常
            try {
                // 释放分布式锁
                unlock(lockKey, lockValue);
            } catch (Exception e) {
                // 释放锁失败不影响主逻辑，只记录日志，避免掩盖原始异常
                log.error("释放锁失败: lockKey={}", lockKey, e);
            }
        }
    }

    /**
     * 获取任务的实时排名
     * 
     * <p>查询任务在队列中的当前位置，包含正在执行的任务数
     * 
     * @param priorityReqDto 包含任务ID的请求参数
     * @return 当前排队序号，null表示任务已完成或被取消
     * @throws Exception 当任务不存在或无权限时
     */
    @Override
    public Long getTaskRank(Text2ImagePriorityReqDto priorityReqDto) throws Exception {
        String tempId = priorityReqDto.getTempId();
        Long currentUserId = UserUtils.getUser().getId();

        // 先从Redis获取实时排名（包含正在执行的任务数）
        Long rank = redisService.getTaskRank(tempId);
        // 返回实时排名
        return rank;
    }

    /**
     * 查找正在执行的任务
     * 
     * @param tempId 任务ID
     * @return 如果任务正在执行返回任务对象，否则返回null
     */
    private ComfyuiTask findRunningTask(String tempId) {
        log.info("开始查找正在执行的任务: tempId={}", tempId);
        
        // 先尝试从临时占位符获取（优先级最高）
        String tempKey = "run_task_temp_" + tempId;
        String tempJson = stringRedisTemplate.opsForValue().get(tempKey);
        log.info("检查临时占位符: key={}, 存在={}", tempKey, tempJson != null);
        
        if (StrUtil.isNotEmpty(tempJson)) {
            try {
                ComfyuiTask task = JSON.parseObject(tempJson, ComfyuiTask.class);
                log.info("从临时占位符找到任务: tempId={}", tempId);
                return task;
            } catch (Exception e) {
                log.warn("解析临时任务JSON失败: {}", tempId, e);
            }
        }
        
        // 遍历所有run_task_*，查找匹配的任务
        java.util.Set<String> runningKeys = stringRedisTemplate.keys("run_task_*");
        log.info("开始遍历正在执行的任务，总数: {}", runningKeys != null ? runningKeys.size() : 0);
        
        if (runningKeys != null) {
            for (String key : runningKeys) {
                // 跳过临时占位符（已在上面处理）
                if (key.contains("temp_")) {
                    log.debug("跳过临时占位符key: {}", key);
                    continue;
                }
                
                String json = stringRedisTemplate.opsForValue().get(key);
                if (StrUtil.isNotEmpty(json)) {
                    try {
                        ComfyuiTask runningTask = JSON.parseObject(json, ComfyuiTask.class);
                        log.info("检查任务: key={}, taskId={}, 目标tempId={}, 匹配={}", 
                                key, runningTask != null ? runningTask.getId() : "null", tempId, 
                                runningTask != null && tempId.equals(runningTask.getId()));
                        
                        if (runningTask != null && tempId.equals(runningTask.getId())) {
                            log.info("找到匹配的正在执行任务: tempId={}, key={}", tempId, key);
                            return runningTask;
                        }
                    } catch (Exception e) {
                        // 忽略解析异常，继续查找下一个
                        log.debug("解析运行中任务JSON失败: {}", key, e);
                    }
                }
            }
        }
        
        // 未找到正在执行的任务
        log.warn("未找到正在执行的任务: tempId={}", tempId);
        return null;
    }
    
    /**
     * 带重试机制的中断任务
     * 
     * @param tempId 任务ID
     * @param currentUserId 当前用户ID
     * @return 是否中断成功
     */
    private boolean interruptTaskWithRetry(String tempId, Long currentUserId) {
        for (int attempt = 1; attempt <= INTERRUPT_MAX_RETRIES; attempt++) {
            try {
                log.info("用户{}尝试中断任务{}，第{}次尝试", currentUserId, tempId, attempt);
                Response<Void> response = comfyuiApi.interruptTask().execute();
                
                if (response.isSuccessful()) {
                    log.info("用户{}成功中断任务{}（第{}次尝试成功）", currentUserId, tempId, attempt);
                    return true;
                } else {
                    log.warn("用户{}中断任务{}失败（第{}次），HTTP状态码: {}", 
                            currentUserId, tempId, attempt, response.code());
                }
            } catch (Exception e) {
                log.warn("用户{}中断任务{}异常（第{}次）: {}", 
                        currentUserId, tempId, attempt, e.getMessage());
            }
            
            // 如果不是最后一次尝试，等待后重试
            if (attempt < INTERRUPT_MAX_RETRIES) {
                try {
                    Thread.sleep(INTERRUPT_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("中断重试等待被打断");
                    break;
                }
            }
        }
        
        // 所有重试都失败
        log.error("用户{}中断任务{}失败，已重试{}次", currentUserId, tempId, INTERRUPT_MAX_RETRIES);
        return false;
    }
    
}
