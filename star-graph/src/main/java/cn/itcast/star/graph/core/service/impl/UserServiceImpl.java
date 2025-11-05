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
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 用户登录验证（支持用户名或手机号，密码使用BCrypt验证）
     */
    @Override
    public UserLoginResDTO loginByPassword(UserLoginReqDTO dto) {
        // 参数校验（用户名/手机号与密码不能为空）
        if (StrUtil.isBlank(dto.getUsername()) || StrUtil.isBlank(dto.getPassword())) {
            throw new CustomException("用户名或密码不能为空！");
        }
        
        // 查询用户（支持用户名或手机号登录）
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, dto.getUsername()).or().eq(User::getMobile, dto.getUsername());
        User user = baseMapper.selectOne(queryWrapper);
        
        if(user==null){
            throw new CustomException("用户名或密码不正确！");
        }
        
        // 验证密码（使用BCrypt加密比对，防止明文存储）
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())){
            throw new CustomException("用户名或密码不正确！");
        }
        
        // 构造登录响应，生成JWT token（用于后续接口鉴权）
        UserLoginResDTO userLoginResDTO = new UserLoginResDTO();
        userLoginResDTO.setAvatar(user.getAvatar());
        userLoginResDTO.setId(user.getId());
        userLoginResDTO.setName(user.getUsername());
        userLoginResDTO.setToken(JwtUtils.genToken(user));

        return userLoginResDTO;
    }
}
