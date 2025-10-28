// 定义包路径
package cn.itcast.star.graph.core.utils;

// 导入Hutool日期字段枚举，用于日期偏移计算
import cn.hutool.core.date.DateField;
// 导入Hutool日期时间工具类
import cn.hutool.core.date.DateTime;
// 导入Hutool的JWT对象类，用于创建和解析JWT
import cn.hutool.jwt.JWT;
// 导入用户实体类
import cn.itcast.star.graph.core.pojo.User;

/**
 * JWT工具类
 * 
 * <p>基于Hutool工具包，封装JWT token的生成和验证功能
 * 
 * <p>主要功能：
 * <ul>
 *     <li>生成包含用户信息的JWT token</li>
 *     <li>验证token并解析出用户信息</li>
 * </ul>
 * 
 * <p>JWT组成部分：
 * <ul>
 *     <li>Header（头部）- 包含算法和令牌类型</li>
 *     <li>Payload（载荷）- 包含用户ID和用户名</li>
 *     <li>Signature（签名）- 使用密钥对头部和载荷进行签名</li>
 * </ul>
 * 
 * <p>安全特性：
 * <ul>
 *     <li>使用密钥签名，防止token被篡改</li>
 *     <li>token有效期为1年，过期后自动失效</li>
 *     <li>无状态认证，服务端不需要存储session</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
// 工具类声明为public，提供静态方法供其他类调用
public class JwtUtils {

    /** 
     * JWT签名密钥
     * 
     * <p>用于对token进行签名和验证，确保token不被篡改
     * <p>注意：实际生产环境中应使用更复杂的密钥，并存储在配置文件中
     */
    static final String key = "itheima.com";

    /**
     * 生成JWT token
     * 
     * <p>将用户ID和用户名编码到token中，有效期1年
     * 
     * <p>生成流程：
     * <ol>
     *     <li>创建JWT对象</li>
     *     <li>设置载荷：用户ID和用户名</li>
     *     <li>设置过期时间：当前时间+1年</li>
     *     <li>设置签名密钥</li>
     *     <li>签名并生成token字符串</li>
     * </ol>
     * 
     * @param user 用户对象，必须包含ID和用户名
     * @return JWT token字符串，用于客户端认证
     */
    public static String genToken(User user){
        // 使用链式调用构建JWT token
        return JWT.create()
                // 将用户ID存入JWT载荷，键名为"uid"
                .setPayload("uid", user.getId())
                // 将用户名存入JWT载荷，键名为"uname"
                .setPayload("uname", user.getUsername())
                // 设置token过期时间：当前时间偏移1年后（有效期1年）
                .setExpiresAt(DateTime.now().offsetNew(DateField.YEAR, 1))
                // 设置签名密钥（将字符串转为字节数组）
                .setKey(key.getBytes())
                // 使用密钥对JWT进行签名，返回完整的token字符串
                .sign();
    }

    /**
     * 验证并解析JWT token
     * 
     * <p>验证token的有效性并解析出用户信息
     * 
     * <p>解析流程：
     * <ol>
     *     <li>解析token字符串为JWT对象</li>
     *     <li>使用密钥验证签名有效性</li>
     *     <li>从载荷中提取用户ID和用户名</li>
     *     <li>封装为User对象返回</li>
     * </ol>
     * 
     * <p>异常处理：token无效、过期、签名错误时返回null
     * 
     * @param token JWT token字符串，来自客户端请求头
     * @return 用户对象，包含ID和用户名；验证失败返回null
     */
    public static User getToken(String token){
        try {
            // 解析token字符串为JWT对象，并设置密钥进行签名验证
            JWT jwt = JWT.of(token).setKey(key.getBytes());
            // 创建用户对象，用于存放解析出的用户信息
            User user = new User();
            // 从JWT载荷中获取用户ID（键名"uid"），转为Long类型并设置到user对象
            // 注意：getPayload返回Object，需要先转字符串再转Long
            user.setId(Long.valueOf(jwt.getPayload("uid") + ""));
            // 从JWT载荷中获取用户名（键名"uname"），转为字符串并设置到user对象
            user.setUsername(jwt.getPayload("uname") + "");
            // 返回封装好的用户对象
            return user;
        } catch (Exception e) {
            // 捕获所有异常：token格式错误、签名验证失败、过期等
            // 验证失败返回null，调用方可根据null判断token无效
            return null;
        }
    }

}
