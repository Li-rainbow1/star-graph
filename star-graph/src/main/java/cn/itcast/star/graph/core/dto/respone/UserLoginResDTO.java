package cn.itcast.star.graph.core.dto.respone;


import lombok.Data;

/**
 * 用户登录响应DTO
 * 
 * <p>登录成功后返回的用户信息和认证token
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class UserLoginResDTO {
    /**
     * 用户ID
     */
    Long id;
    
    /**
     * 认证token
     * <p>后续请求需要在Header中带上此token
     */
    private String token;
    
    /**
     * 用户名
     */
    private String name;
    
    /**
     * 用户头像 URL
     */
    private String avatar;
    
    /**
     * VIP等级
     */
    private Integer vipLevel;
    
    /**
     * 用户积分余额
     */
    private Integer standing;
}
