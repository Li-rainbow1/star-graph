package cn.itcast.star.graph.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文生图服务实现类
 * 
 * <p>实现文生图的核心业务逻辑，包括：
 * <ul>
 *     <li>创建文生图任务并加入队列</li>
 *     <li>取消排队中的任务</li>
 *     <li>任务插队（提升优先级）</li>
 *     <li>查询用户的生图历史</li>
 * </ul>
 * 
 * <p>关键技术点：
 * <ul>
 *     <li>使用分布式锁防止并发操作冲突</li>
 *     <li>Redis ZSet实现优先级队列</li>
 *     <li>积分冻结/扣除/归还机制</li>
 *     <li>Ollama AI翻译提示词</li>
 *     <li>Freemarker生成ComfyUI工作流</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
@Slf4j  // Lombok注解，自动生成日志对象log
@Service  // Spring Service组件注解
public class Text2ImageServiceImpl implements Text2ImageService {
    // ==================== 依赖注入 ====================
    
    @Autowired  // 注入Ollama服务，用于中英文翻译
    OllamaService ollamaService;
    
    @Autowired  // 注入Freemarker服务，用于生成ComfyUI工作流JSON
    FreemarkerService freemarkerService;
    
    @Autowired  // 注入Redis服务，用于任务队列管理
    RedisService redisService;
    
    @Autowired  // 注入用户资金服务，用于积分冻结/扣除/归还
    UserFundRecordService userFundRecordService;
    
    @Autowired  // 注入用户结果服务，用于保存生成的图片记录
    UserResultService userResultService;
    
    @Autowired  // 注入Redis模板，用于实现分布式锁
    StringRedisTemplate stringRedisTemplate;
    
    // ==================== 常量定义 ====================
    
    /** 分布式锁的key前缀，用于防止并发操作同一任务 */
    private static final String LOCK_KEY_PREFIX = "lock:task:";
    
    /** 分布式锁的超时时间（秒），防止死锁 */
    private static final long LOCK_TIMEOUT = 10;
    
    /** 任务插队消耗的积分数 */
    private static final int PRIORITY_COST = 5;
    
    /** 提升优先级时减少的分值（ZSet score越小优先级越高） */
    private static final double PRIORITY_INCREMENT = 10.0;
    
    /** 分页查询的最小页码 */
    private static final int MIN_PAGE_SIZE = 1;
    
    /** 分页查询的最大每页数量 */
    private static final int MAX_PAGE_SIZE = 20;
    
    /** 分页查询的默认每页数量 */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 把请求参数封装成comfyui的请求对象
     *
     * @param text2ImageReqDto
     * @return
     * @throws Exception
     */
    public ComfyuiTask getComfyuiTask(Text2ImageReqDto text2ImageReqDto) throws Exception {
        // 创建ComfyUI模型参数对象
        ComfyuiModel comfyuiModel = new ComfyuiModel();
        // 将请求参数复制到模型对象（忽略空值）
        BeanUtil.copyProperties(text2ImageReqDto, comfyuiModel, true);
        // 根据用户选择的模型编号，设置对应的模型文件名
        comfyuiModel.setModelName(text2ImageReqDto.modelName());
        // 根据用户选择的采样器编号，设置对应的采样器名称
        comfyuiModel.setSamplerName(text2ImageReqDto.samplerName());
        // 设置调度器名称（固定使用karras）
        comfyuiModel.setScheduler(text2ImageReqDto.scheduler());
        // 根据用户选择的尺寸比例，计算实际图片宽度
        comfyuiModel.setWidth(text2ImageReqDto.width());
        // 根据用户选择的尺寸比例，计算实际图片高度
        comfyuiModel.setHeight(text2ImageReqDto.height());

        // 处理正向提示词：添加画质增强前缀 + 使用Ollama翻译用户输入的中文提示词为英文
        comfyuiModel.setPropmt("(8k, best quality, masterpiece),(high detailed skin)," + ollamaService.translate(text2ImageReqDto.getPropmt()));
        // 处理负向提示词：翻译用户输入 + 添加常见负面词汇（避免生成低质量图片）
        comfyuiModel.setReverse(ollamaService.translate(text2ImageReqDto.getReverse()) + ",bad face,naked,bad finger,bad arm,bad leg,bad eye");

        // 使用Freemarker模板引擎，根据模型参数生成ComfyUI工作流JSON字符串
        String prompt = freemarkerService.renderText2Image(comfyuiModel);
        // 将JSON字符串解析为对象，构建ComfyUI请求DTO（包含客户端ID和工作流配置）
        ComfyuiRequestDto comfyuiRequestDto = new ComfyuiRequestDto(Constants.COMFYUI_CLIENT_ID, JSON.parseObject(prompt));

        // 创建ComfyUI任务对象，传入WebSocket客户端ID和请求配置
        ComfyuiTask comfyuiTask = new ComfyuiTask(text2ImageReqDto.getClientId(), comfyuiRequestDto);
        // 从ThreadLocal中获取当前登录用户ID并设置到任务对象
        comfyuiTask.setUserId(UserUtils.getUser().getId());
        // 设置需要生成的图片数量
        comfyuiTask.setSize(text2ImageReqDto.getSize());
        // 返回封装好的任务对象
        return comfyuiTask;
    }

    /**
     * 文生图接口实现
     *
     * @param text2ImageReqDto
     * @return
     * @throws Exception
     */
    @Override
    public Text2ImageResDto textToImage(Text2ImageReqDto text2ImageReqDto) throws Exception {
        // 验证生成图片数量参数，必须大于等于1
        if (text2ImageReqDto.getSize() < 1) {
            throw new CustomException("请求参数错误！");
        }
        
        // 获取当前用户ID
        Long userId = UserUtils.getUser().getId();
        
        // 先冻结用户积分（按图片数量扣除），防止用户积分不足或恶意提交
        userFundRecordService.pointsFreeze(userId, text2ImageReqDto.getSize());
        
        try {
            // 将用户请求封装为ComfyUI任务对象（包含翻译提示词、生成工作流等）
            ComfyuiTask comfyuiTask = getComfyuiTask(text2ImageReqDto);
            // 将任务添加到Redis优先级队列，返回带有队列位置的任务对象
            comfyuiTask = redisService.addQueueTask(comfyuiTask);
            // 构造响应对象
            Text2ImageResDto text2ImageResDto = new Text2ImageResDto();
            // 设置任务临时ID（用于后续取消、插队等操作）
            text2ImageResDto.setPid(comfyuiTask.getId());
            // 设置任务在队列中的序号（告知用户前面还有多少任务）
            text2ImageResDto.setQueueIndex(comfyuiTask.getIndex());
            // 返回响应给前端
            return text2ImageResDto;
        } catch (Exception e) {
            // 【Bug修复】如果创建任务或加入队列失败（如：Ollama翻译失败、Freemarker渲染失败）
            // 必须归还已冻结的积分，否则积分会永久冻结
            log.error("创建文生图任务失败，归还用户{}的积分{}", userId, text2ImageReqDto.getSize(), e);
            userFundRecordService.freezeReturn(userId, text2ImageReqDto.getSize());
            throw e;
        }
    }

    /**
     * 取消文生图任务（使用分布式锁保证并发安全）
     *
     * @param cancelReqDto
     * @throws Exception
     */
    @Override
    public void cancelTask(Text2ImageCancelReqDto cancelReqDto) throws Exception {
        // 从请求DTO中获取任务临时ID
        String tempId = cancelReqDto.getTempId();
        // 校验任务ID不能为空
        if (tempId == null || tempId.trim().isEmpty()) {
            throw new CustomException("任务ID不能为空");
        }
        
        // 从ThreadLocal中获取当前登录用户ID（只获取一次，避免重复调用）
        Long currentUserId = UserUtils.getUser().getId();
        // 记录取消操作日志
        log.info("用户{}尝试取消任务{}", currentUserId, tempId);
        
        // 构造分布式锁的key（每个任务一个锁）
        String lockKey = LOCK_KEY_PREFIX + tempId;
        // 生成随机锁值，用于释放时验证是否是自己的锁
        String lockValue = UUID.randomUUID().toString();
        
        // 尝试获取分布式锁，防止并发取消同一任务
        if (!tryLock(lockKey, lockValue)) {
            // 获取锁失败，说明有其他请求正在处理该任务
            log.warn("用户{}取消任务{}失败: 获取锁失败", currentUserId, tempId);
            throw new CustomException("操作过于频繁，请稍后再试");
        }
        
        // 标记任务是否已从Redis删除，用于异常时判断是否需要重新加入队列或归还积分
        boolean taskRemoved = false;
        ComfyuiTask queueTask = null;
        
        try {
            // 验证任务是否存在、是否已开始、当前用户是否有权限操作
            queueTask = validateTaskAndPermission(tempId, currentUserId);
            
            // 从Redis ZSet队列和String存储中删除任务
            boolean removed = redisService.removeQueueTask(tempId);
            // 判断删除是否成功
            if (!removed) {
                // 删除失败，记录错误日志
                log.error("用户{}取消任务{}失败: Redis删除失败", currentUserId, tempId);
                throw new CustomException("任务取消失败");
            }
            
            taskRemoved = true;  // 标记任务已删除
            
            // 任务取消成功，将冻结的积分返还到用户可用账户
            userFundRecordService.freezeReturn(queueTask.getUserId(), queueTask.getSize());
            // 记录成功日志，包含归还的积分数量
            log.info("用户{}成功取消任务{}，归还积分{}", currentUserId, tempId, queueTask.getSize());
        } catch (CustomException e) {
            // 业务异常（如：任务不存在、无权限等）直接抛出，不记录堆栈
            throw e;
        } catch (Exception e) {
            // 【Bug修复】系统异常时，如果任务已删除但积分归还失败，必须确保积分归还
            if (taskRemoved && queueTask != null) {
                log.error("用户{}取消任务{}成功，但归还积分时发生异常，重试归还", currentUserId, tempId, e);
                try {
                    userFundRecordService.freezeReturn(queueTask.getUserId(), queueTask.getSize());
                    log.info("重试归还积分成功，用户{}积分{}", queueTask.getUserId(), queueTask.getSize());
                } catch (Exception refundException) {
                    // 归还积分失败，记录严重错误，需要人工介入
                    log.error("取消任务后归还积分失败！用户{}需要人工补偿积分{}，任务已从队列删除", 
                            queueTask.getUserId(), queueTask.getSize(), refundException);
                }
            } else {
                // 任务未删除前的异常，直接抛出
                log.error("用户{}取消任务{}发生系统异常", currentUserId, tempId, e);
            }
            throw new CustomException("任务取消失败");
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
     * 获取用户文生图历史列表
     *
     * @param listReqDto
     * @return
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
        // 设置查询条件：只查询当前用户的记录
        queryWrapper.eq(UserResult::getUserId, userId)
                    // 按创建时间降序排序（最新的在最前）
                    .orderByDesc(UserResult::getCreatedTime);
        
        // 执行分页查询，返回分页结果对象
        IPage<UserResult> resultPage = userResultService.page(page, queryWrapper);
        
        // 将 MyBatis Plus 的分页结果转换为自定义的 PageResult 对象返回
        // 包含总数量和当前页的记录列表
        return PageResult.ok(resultPage.getTotal(), resultPage.getRecords());
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
     * 验证任务并检查权限（通用方法）
     * 
     * @param tempId 任务ID
     * @param currentUserId 当前用户ID（可选，传入null则内部获取）
     * @return 任务对象
     */
    private ComfyuiTask validateTaskAndPermission(String tempId, Long currentUserId) {
        // 第一步：检查任务在队列中的排名（判断任务是否已经开始执行）
        Long currentRank = redisService.getTaskRank(tempId);
        // 如果排名为null，说明任务已经从队列中弹出（正在执行或已完成）
        if (currentRank == null) {
            throw new CustomException("任务已经开始或不存在");
        }
        
        // 第二步：获取任务的详细信息
        ComfyuiTask queueTask = redisService.getQueueTask(tempId);
        // 如果任务不存在，抛出异常
        if (queueTask == null) {
            throw new CustomException("任务不存在");
        }
        
        // 第三步：验证当前用户是否有权限操作该任务
        if (currentUserId == null) {
            // 如果没有传入用户ID，从ThreadLocal获取
            currentUserId = UserUtils.getUser().getId();
        }
        // 对比任务的所有者ID和当前用户ID
        if (!currentUserId.equals(queueTask.getUserId())) {
            // 如果不是任务的所有者，无权操作
            throw new CustomException("无权限操作该任务");
        }
        
        // 验证通过，返回任务对象
        return queueTask;
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
            
            // 先扣除积分，再提升优先级，避免用户未付费但优先级已提升
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

        // 先从Redis获取实时排名
        Long rank = redisService.getTaskRank(tempId);
        
        // 如果任务不在队列中也不在执行中（已完成或被取消），返回null
        if (rank == null) {
            return null;
        }
        
        // 任务可能在队列中，也可能正在执行
        // 先尝试从队列获取
        ComfyuiTask task = redisService.getQueueTask(tempId);
        
        // 如果队列中没有，可能正在执行，从执行任务中获取
        if (task == null) {
            // 尝试从临时占位符获取
            String tempJson = stringRedisTemplate.opsForValue().get("run_task_temp_" + tempId);
            if (StrUtil.isNotEmpty(tempJson)) {
                task = JSON.parseObject(tempJson, ComfyuiTask.class);
            } else {
                // 遍历所有run_task_*，查找匹配的任务
                java.util.Set<String> runningKeys = stringRedisTemplate.keys("run_task_*");
                if (runningKeys != null) {
                    for (String key : runningKeys) {
                        if (key.contains("temp_")) continue;
                        String json = stringRedisTemplate.opsForValue().get(key);
                        if (StrUtil.isNotEmpty(json)) {
                            try {
                                ComfyuiTask runningTask = JSON.parseObject(json, ComfyuiTask.class);
                                if (runningTask != null && tempId.equals(runningTask.getId())) {
                                    task = runningTask;
                                    break;
                                }
                            } catch (Exception e) {
                                // 忽略解析异常
                            }
                        }
                    }
                }
            }
        }
        
        // 如果任务不存在，返回null
        if (task == null) {
            return null;
        }
        
        // 检查权限
        if (!currentUserId.equals(task.getUserId())) {
            throw new CustomException("无权限查询该任务");
        }

        // 返回实时排名
        return rank;
    }
}
