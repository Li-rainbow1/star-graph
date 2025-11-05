package cn.itcast.star.graph.core.service;

import cn.itcast.star.graph.core.pojo.UserResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户生图结果服务 - 管理用户的文生图历史记录
 */
public interface UserResultService extends IService<UserResult> {

    /**
     * 批量保存图片URL到用户历史记录
     */
    public void saveList(List<String> urls, Long userId);

}