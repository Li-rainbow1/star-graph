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
 * 用户控制器 - 提供用户登录等接口
 */
@RestController
@RequestMapping("/api/1.0/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户密码登录，返回token和用户信息
     */
    @PostMapping("/login")
    public Result<UserLoginResDTO> login(@RequestBody UserLoginReqDTO dto){
        UserLoginResDTO userLoginResponeDto = userService.loginByPassword(dto);
        return Result.ok(userLoginResponeDto);
    }
}
