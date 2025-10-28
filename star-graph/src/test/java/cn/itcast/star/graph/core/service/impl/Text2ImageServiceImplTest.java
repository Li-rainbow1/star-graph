package cn.itcast.star.graph.core.service.impl;

import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiTask;
import cn.itcast.star.graph.core.dto.common.PageResult;
import cn.itcast.star.graph.core.dto.request.Text2ImageCancelReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImageListReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImagePriorityReqDto;
import cn.itcast.star.graph.core.exception.CustomException;
import cn.itcast.star.graph.core.pojo.User;
import cn.itcast.star.graph.core.pojo.UserResult;
import cn.itcast.star.graph.core.service.RedisService;
import cn.itcast.star.graph.core.service.UserFundRecordService;
import cn.itcast.star.graph.core.service.UserResultService;
import cn.itcast.star.graph.core.utils.UserUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Text2ImageServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class Text2ImageServiceImplTest {

    @Mock
    private RedisService redisService;

    @Mock
    private UserFundRecordService userFundRecordService;

    @Mock
    private UserResultService userResultService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private Text2ImageServiceImpl text2ImageService;

    private User mockUser;
    private ComfyuiTask mockTask;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");

        mockTask = new ComfyuiTask("clientId123", null);
        mockTask.setId("task123");
        mockTask.setUserId(1L);
        mockTask.setSize(10);
        mockTask.setIndex(5L);

        // Mock StringRedisTemplate
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /**
     * 测试取消任务 - 成功场景
     */
    @Test
    void testCancelTask_Success() throws Exception {
        // Given
        Text2ImageCancelReqDto reqDto = new Text2ImageCancelReqDto();
        reqDto.setTempId("task123");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getUser).thenReturn(mockUser);

            // Mock 分布式锁
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            // Mock 任务验证
            when(redisService.getTaskRank("task123")).thenReturn(5L);
            when(redisService.getQueueTask("task123")).thenReturn(mockTask);

            // Mock 删除任务
            when(redisService.removeQueueTask("task123")).thenReturn(true);

            // When
            text2ImageService.cancelTask(reqDto);

            // Then
            verify(redisService).removeQueueTask("task123");
            verify(userFundRecordService).freezeReturn(1L, 10);
        }
    }

    /**
     * 测试取消任务 - 任务ID为空
     */
    @Test
    void testCancelTask_EmptyTaskId() {
        // Given
        Text2ImageCancelReqDto reqDto = new Text2ImageCancelReqDto();
        reqDto.setTempId("");

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            text2ImageService.cancelTask(reqDto);
        });

        assertEquals("任务ID不能为空", exception.getMessage());
    }

    /**
     * 测试取消任务 - 获取锁失败
     */
    @Test
    void testCancelTask_LockFailed() {
        // Given
        Text2ImageCancelReqDto reqDto = new Text2ImageCancelReqDto();
        reqDto.setTempId("task123");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getUser).thenReturn(mockUser);

            // Mock 分布式锁失败
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(false);

            // When & Then
            CustomException exception = assertThrows(CustomException.class, () -> {
                text2ImageService.cancelTask(reqDto);
            });

            assertEquals("操作过于频繁，请稍后再试", exception.getMessage());
        }
    }

    /**
     * 测试取消任务 - 任务已经开始
     */
    @Test
    void testCancelTask_TaskAlreadyStarted() {
        // Given
        Text2ImageCancelReqDto reqDto = new Text2ImageCancelReqDto();
        reqDto.setTempId("task123");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getUser).thenReturn(mockUser);

            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            // Mock 任务不在队列中
            when(redisService.getTaskRank("task123")).thenReturn(null);

            // When & Then
            CustomException exception = assertThrows(CustomException.class, () -> {
                text2ImageService.cancelTask(reqDto);
            });

            assertEquals("任务已经开始或不存在", exception.getMessage());
        }
    }

    /**
     * 测试取消任务 - 无权限操作
     */
    @Test
    void testCancelTask_NoPermission() {
        // Given
        Text2ImageCancelReqDto reqDto = new Text2ImageCancelReqDto();
        reqDto.setTempId("task123");

        ComfyuiTask otherUserTask = new ComfyuiTask("clientId456", null);
        otherUserTask.setId("task123");
        otherUserTask.setUserId(999L); // 不同的用户ID

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getUser).thenReturn(mockUser);

            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            when(redisService.getTaskRank("task123")).thenReturn(5L);
            when(redisService.getQueueTask("task123")).thenReturn(otherUserTask);

            // When & Then
            CustomException exception = assertThrows(CustomException.class, () -> {
                text2ImageService.cancelTask(reqDto);
            });

            assertEquals("无权限操作该任务", exception.getMessage());
        }
    }

    /**
     * 测试插队 - 成功场景
     */
    @Test
    void testIncreasePriority_Success() throws Exception {
        // Given
        Text2ImagePriorityReqDto reqDto = new Text2ImagePriorityReqDto();
        reqDto.setTempId("task123");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getUser).thenReturn(mockUser);

            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            // Mock 当前排名
            when(redisService.getTaskRank("task123"))
                    .thenReturn(5L)  // 当前排名
                    .thenReturn(2L); // 插队后排名

            when(redisService.getQueueTask("task123")).thenReturn(mockTask);
            when(redisService.increasePriority("task123", 10.0)).thenReturn(true);

            // When
            Long newRank = text2ImageService.increasePriority(reqDto);

            // Then
            assertEquals(2L, newRank);
            verify(redisService).increasePriority("task123", 10.0);
            verify(userFundRecordService).directDeduction(1L, 5);
        }
    }

    /**
     * 测试插队 - 已经是第一名
     */
    @Test
    void testIncreasePriority_AlreadyFirst() {
        // Given
        Text2ImagePriorityReqDto reqDto = new Text2ImagePriorityReqDto();
        reqDto.setTempId("task123");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getUser).thenReturn(mockUser);

            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            // Mock 当前排名为1
            when(redisService.getTaskRank("task123")).thenReturn(1L);

            // When & Then
            CustomException exception = assertThrows(CustomException.class, () -> {
                text2ImageService.increasePriority(reqDto);
            });

            assertEquals("当前任务已经是第一名，无需插队", exception.getMessage());
        }
    }

    /**
     * 测试插队 - Redis提升优先级失败
     */
    @Test
    void testIncreasePriority_RedisFailure() {
        // Given
        Text2ImagePriorityReqDto reqDto = new Text2ImagePriorityReqDto();
        reqDto.setTempId("task123");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getUser).thenReturn(mockUser);

            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            when(redisService.getTaskRank("task123")).thenReturn(5L);
            when(redisService.getQueueTask("task123")).thenReturn(mockTask);
            when(redisService.increasePriority("task123", 10.0)).thenReturn(false);

            // When & Then
            CustomException exception = assertThrows(CustomException.class, () -> {
                text2ImageService.increasePriority(reqDto);
            });

            assertEquals("提升优先级失败", exception.getMessage());
        }
    }

    /**
     * 测试获取用户图片列表 - 成功场景
     */
    @Test
    void testGetUserImageList_Success() {
        // Given
        Text2ImageListReqDto reqDto = new Text2ImageListReqDto();
        reqDto.setPageNum(1);
        reqDto.setPageSize(10);

        UserResult result1 = new UserResult();
        result1.setId(1L);
        result1.setUserId(1L);

        UserResult result2 = new UserResult();
        result2.setId(2L);
        result2.setUserId(1L);

        List<UserResult> resultList = Arrays.asList(result1, result2);

        Page<UserResult> mockPage = new Page<>(1, 10);
        mockPage.setRecords(resultList);
        mockPage.setTotal(2);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getUser).thenReturn(mockUser);

            when(userResultService.page(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            // When
            PageResult<List<UserResult>> result = text2ImageService.getUserImageList(reqDto);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getTotal());
            assertEquals(2, result.getData().size());
        }
    }

    /**
     * 测试获取用户图片列表 - 分页参数校验
     */
    @Test
    void testGetUserImageList_PageParamValidation() {
        // Given - 无效的分页参数
        Text2ImageListReqDto reqDto = new Text2ImageListReqDto();
        reqDto.setPageNum(0);  // 无效
        reqDto.setPageSize(100); // 超过最大值

        Page<UserResult> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList());
        mockPage.setTotal(0);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getUser).thenReturn(mockUser);

            when(userResultService.page(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            // When
            PageResult<List<UserResult>> result = text2ImageService.getUserImageList(reqDto);

            // Then - 参数应该被修正
            assertNotNull(result);
            verify(userResultService).page(argThat(page -> 
                page.getCurrent() == 1 && page.getSize() == 10
            ), any(LambdaQueryWrapper.class));
        }
    }
}
