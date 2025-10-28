package cn.itcast.star.graph.core.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.itcast.star.graph.core.dto.request.UserLoginReqDTO;
import cn.itcast.star.graph.core.dto.respone.UserLoginResDTO;
import cn.itcast.star.graph.core.exception.CustomException;
import cn.itcast.star.graph.core.mapper.UserMapper;
import cn.itcast.star.graph.core.service.UserService;
import cn.itcast.star.graph.core.utils.JwtUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.itcast.star.graph.core.pojo.User;
import org.springframework.stereotype.Service;

/**
 * @author Administrator
 * @description 针对表【sg_user(用户信息表)】的数据库操作Service实现
 * @createDate 2025-10-19 21:06:12
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 用户登录的业务逻辑
     *
     * @param dto
     * @return
     */
    @Override
    public UserLoginResDTO loginByPassword(UserLoginReqDTO dto) {
        // 第一步：参数校验，检查用户名和密码是否为空
        if (StrUtil.isBlank(dto.getUsername()) || StrUtil.isBlank(dto.getPassword())) {
            // 如果用户名或密码为空，抛出自定义异常
            throw new CustomException("用户名或密码不能为空！");
        }
        // 第二步：构建查询条件，支持用户名或手机号登录
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        // 设置查询条件：用户名匹配 或 手机号匹配（支持两种方式登录）
        queryWrapper.eq(User::getUsername, dto.getUsername()).or().eq(User::getMobile, dto.getUsername());
        // 执行数据库查询，获取用户信息
        User user = baseMapper.selectOne(queryWrapper);
        // 如果查询不到用户，说明用户名不存在
        if(user==null){
            // 为了安全，不明确告知是用户名还是密码错误
            throw new CustomException("用户名或密码不正确！");
        }
        // 第三步：验证密码是否正确（数据库中存储的是BCrypt加密后的密码）
        // 使用BCrypt.checkpw方法验证明文密码和加密密码是否匹配
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())){
            // 密码不匹配，抛出异常
            throw new CustomException("用户名或密码不正确！");
        }
        // 第四步：登录成功，构造响应对象
        UserLoginResDTO userLoginResDTO = new UserLoginResDTO();
        // 设置用户头像URL
        userLoginResDTO.setAvatar(user.getAvatar());
        // 设置用户ID
        userLoginResDTO.setId(user.getId());
        // 设置用户名
        userLoginResDTO.setName(user.getUsername());
        // 生成JWT token，包含用户ID和用户名，有效期1年
        userLoginResDTO.setToken(JwtUtils.genToken(user));

        // 返回登录响应对象给前端
        return userLoginResDTO;
    }
}
