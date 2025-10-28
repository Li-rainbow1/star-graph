package cn.itcast.star.graph.core.dto.request;


import lombok.Data;

/**
 * 用户登录请求DTO
 * 
 * <p>用户通过用户名和密码登录系统
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class UserLoginReqDTO {
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
}
