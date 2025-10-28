package cn.itcast.star.graph.core.service;

import cn.itcast.star.graph.core.dto.request.UserLoginReqDTO;
import cn.itcast.star.graph.core.dto.respone.UserLoginResDTO;
import cn.itcast.star.graph.core.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

/**
 * 用户服务接口
 * 
 * <p>提供用户相关的业务逻辑，包括用户登录、用户信息管理等
 * <p>针对表【sg_user(用户信息表)】的数据库操作Service
 * 
 * @author itcast
 * @since 1.0
 */
public interface UserService extends IService<User> {

    /**
     * 用户密码登录
     * 
     * <p>验证用户名和密码，成功后生成token并返回用户信息
     * 
     * @param dto 登录请求参数，包含用户名和密码
     * @return 登录响应，包含用户信息和token
     */
    UserLoginResDTO loginByPassword(UserLoginReqDTO dto);
}
