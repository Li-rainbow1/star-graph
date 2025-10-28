package cn.itcast.star.graph.core.service.impl;

import cn.itcast.star.graph.core.mapper.UserResultMapper;
import cn.itcast.star.graph.core.pojo.UserResult;
import cn.itcast.star.graph.core.service.UserFundRecordService;
import cn.itcast.star.graph.core.service.UserResultService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * sg_user_result Service 接口实现
 * </p>
 *
 * @author luoxu
 * @since 2024-10-18 16:07:29
 */
@Service
@Transactional
@Slf4j
public class UserResultServiceImpl extends ServiceImpl<UserResultMapper, UserResult> implements UserResultService {
    @Autowired
    UserFundRecordService userFundRecordService;

    @Override
    public void saveList(List<String> urls, Long userId) {
        // 使用Stream将图片URL列表转换为UserResult实体列表
        List<UserResult> userResults = urls.stream().map((url) -> {
            // 为每个URL创建一个UserResult对象
            UserResult userResult = new UserResult();
            // 设置用户ID，标识这张图片属于哪个用户
            userResult.setUserId(userId);
            // 设置图片的访问URL
            userResult.setUrl(url);
            // 设置收藏状态为0（未收藏）
            userResult.setCollect(0);
            // 返回构建好的UserResult对象
            return userResult;
        }).collect(Collectors.toList());
        // 批量保存所有图片记录到数据库（提高插入效率）
        this.saveBatch(userResults);
        // 【Bug修复】移除重复的积分扣除，已在ComfyuiMessageServiceImpl.handleExecutedMessage中处理
    }
}