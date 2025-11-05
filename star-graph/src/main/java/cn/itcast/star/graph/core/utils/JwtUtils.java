package cn.itcast.star.graph.core.utils;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.jwt.JWT;
import cn.itcast.star.graph.core.pojo.User;

/**
 * JWT工具类 - 生成和验证JWT token（有效期1年）
 */
public class JwtUtils {

    // JWT签名密钥
    static final String key = "itheima.com";

    /**
     * 生成JWT token，包含用户ID和用户名
     */
    public static String genToken(User user){
        return JWT.create()
                .setPayload("uid", user.getId())
                .setPayload("uname", user.getUsername())
                .setExpiresAt(DateTime.now().offsetNew(DateField.YEAR, 1))
                .setKey(key.getBytes())
                .sign();
    }

    /**
     * 验证并解析JWT token，失败返回null
     */
    public static User getToken(String token){
        try {
            JWT jwt = JWT.of(token).setKey(key.getBytes());
            User user = new User();
            user.setId(Long.valueOf(jwt.getPayload("uid") + ""));
            user.setUsername(jwt.getPayload("uname") + "");
            return user;
        } catch (Exception e) {
            return null;
        }
    }

}
