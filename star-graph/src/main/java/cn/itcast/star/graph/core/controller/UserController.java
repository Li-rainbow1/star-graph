package cn.itcast.star.graph.core.controller;

import cn.itcast.star.graph.core.dto.common.Result;
import cn.itcast.star.graph.core.dto.request.UserLoginReqDTO;
import cn.itcast.star.graph.core.dto.respone.UserLoginResDTO;
import cn.itcast.star.graph.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 * 
 * <p>提供用户相关的HTTP接口，包括用户登录等功能
 * <p>路径前缀：/api/1.0/user
 * 
 * @author itcast
 * @since 1.0
 */
@RestController
@RequestMapping("/api/1.0/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * 
     * <p>通过用户名和密码进行登录验证，成功后返回用户信息和token
     * 
     * @param dto 登录请求参数，包含用户名和密码
     * @return 登录响应，包含用户信息和认证token
     */
    @PostMapping("/login")
    public Result<UserLoginResDTO> login(@RequestBody UserLoginReqDTO dto){
        UserLoginResDTO userLoginResponeDto = userService.loginByPassword(dto);
        return Result.ok(userLoginResponeDto);
    }
}
