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
 * 用户生图结果服务实现
 */
@Service
@Transactional
@Slf4j
public class UserResultServiceImpl extends ServiceImpl<UserResultMapper, UserResult> implements UserResultService {
    @Autowired
    UserFundRecordService userFundRecordService;

    /**
     * 批量保存图片URL到用户历史记录
     */
    @Override
    public void saveList(List<String> urls, Long userId) {
        List<UserResult> userResults = urls.stream().map((url) -> {
            UserResult userResult = new UserResult();
            userResult.setUserId(userId);
            userResult.setUrl(url);
            userResult.setCollect(0);
            return userResult;
        }).collect(Collectors.toList());
        this.saveBatch(userResults);
    }
}