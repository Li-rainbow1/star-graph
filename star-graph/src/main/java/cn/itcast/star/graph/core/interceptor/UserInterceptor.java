// 定义包路径
package cn.itcast.star.graph.core.interceptor;

// 导入Hutool字符串工具类，用于判断字符串是否为空
import cn.hutool.core.util.StrUtil;
// 导入统一响应结果类
import cn.itcast.star.graph.core.dto.common.Result;
// 导入响应结果状态码枚举
import cn.itcast.star.graph.core.dto.common.ResultCode;
// 导入用户实体类
import cn.itcast.star.graph.core.pojo.User;
// 导入JWT工具类，用于解析token
import cn.itcast.star.graph.core.utils.JwtUtils;
// 导入用户工具类，用于ThreadLocal存储和获取用户信息
import cn.itcast.star.graph.core.utils.UserUtils;
// 导入FastJSON工具类，用于将对象转换为JSON字符串
import com.alibaba.fastjson2.JSON;
// 导入Jakarta Servlet的HttpServletRequest接口
import jakarta.servlet.http.HttpServletRequest;
// 导入Jakarta Servlet的HttpServletResponse接口
import jakarta.servlet.http.HttpServletResponse;
// 导入Spring的HTTP请求头常量类
import org.springframework.http.HttpHeaders;
// 导入Spring的HTTP状态码枚举
import org.springframework.http.HttpStatus;
// 导入Spring的媒体类型常量类
import org.springframework.http.MediaType;
// 导入Spring MVC的拦截器接口
import org.springframework.web.servlet.HandlerInterceptor;

// 导入IO异常类
import java.io.IOException;
// 导入PrintWriter类，用于向响应写入数据
import java.io.PrintWriter;

/**
 * 用户认证拦截器
 * 
 * <p>拦截需要认证的请求，从请求头中解析JWT token，验证用户身份
 * <p>主要功能：
 * <ul>
 *     <li>从请求头Authorization中获取token</li>
 *     <li>验证token有效性</li>
 *     <li>解析用户信息并存入ThreadLocal</li>
 *     <li>请求结束后清理ThreadLocal</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
public class UserInterceptor implements HandlerInterceptor {

    /**
     * 请求预处理方法（在Controller方法执行之前调用）
     * 
     * <p>拦截器的核心方法，用于验证用户身份
     * 
     * <p>执行流程：
     * <ol>
     *     <li>从请求头获取JWT token</li>
     *     <li>校验token是否为空</li>
     *     <li>解析token获取用户信息</li>
     *     <li>将用户信息存入ThreadLocal</li>
     * </ol>
     * 
     * @param request HTTP请求对象，用于获取请求头和URI
     * @param response HTTP响应对象，用于返回错误信息
     * @param handler 处理器对象（Controller方法）
     * @return true-放行请求，继续执行后续流程；false-拦截请求，不再执行Controller
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // 从HTTP请求头中获取Authorization字段的值（JWT token）
            // 客户端应在请求头中携带：Authorization: Bearer <token>
            String token = request.getHeader(HttpHeaders.AUTHORIZATION);
            
            // 判断token是否为空（null或空字符串）
            if (StrUtil.isEmpty(token)){
                // token为空，说明用户未登录或未携带token
                // 向客户端返回401未授权响应
                writeAuthorizationedFailed(response);
                // 返回false，拦截请求，不再执行后续Controller方法
                return false;
            }
            
            // 使用JwtUtils工具类解析token，获取用户信息（ID和用户名）
            // 如果token无效、过期或签名错误，会返回null
            User user = JwtUtils.getToken(token);
            
            // 判断解析后的用户对象是否为null
            if (user==null){
                // token无效（过期、篡改或格式错误）
                // 向客户端返回401未授权响应
                writeAuthorizationedFailed(response);
                // 返回false，拦截请求
                return false;
            }
            
            // token有效，将用户信息存储到ThreadLocal中
            // 后续的Controller和Service可以通过UserUtils.getUser()获取当前用户信息
            UserUtils.saveUser(user);
            
            // 返回true，放行请求，继续执行Controller方法
            return true;
        } catch (Exception e) {
            // 发生异常时清理ThreadLocal，防止内存泄漏
            UserUtils.removeUser();
            throw e;
        }
    }

    /**
     * 向客户端返回认证失败的响应信息
     * 
     * <p>当token为空或无效时调用此方法，返回401状态码和JSON格式的错误信息
     * 
     * <p>响应格式：
     * <pre>
     * {
     *   "code": 401,
     *   "message": "访问未授权",
     *   "data": null
     * }
     * </pre>
     * 
     * @param response HTTP响应对象，用于写入错误信息
     */
    private void writeAuthorizationedFailed(HttpServletResponse response) {
        // 设置HTTP响应状态码为401（UNAUTHORIZED - 未授权）
        // HttpStatus.UNAUTHORIZED.value()返回整数401
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        
        // 设置响应内容类型为JSON格式
        // MediaType.APPLICATION_JSON_VALUE = "application/json"
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // 设置响应字符编码为UTF-8，支持中文等多语言
        response.setCharacterEncoding("UTF-8");

        // 构建统一的失败响应对象
        // ResultCode.ACCESS_UNAUTHORIZED是预定义的401错误码和错误消息
        Result<Object> failed = Result.failed(ResultCode.ACCESS_UNAUTHORIZED);
        
        // 将失败响应对象转换为JSON字符串
        String jsonString = JSON.toJSONString(failed);
        
        try {
            // 获取响应的输出流Writer对象
            PrintWriter writer = response.getWriter();
            // 将JSON字符串写入响应体
            writer.write(jsonString);
            // 刷新输出流，确保数据立即发送到客户端
            writer.flush();
        } catch (IOException e) {
            // 如果写入响应时发生IO异常，包装为运行时异常抛出
            // 这通常不会发生，除非网络连接异常中断
            throw new RuntimeException(e);
        }

    }


    /**
     * 请求完成后的清理方法（在视图渲染完成后调用）
     * 
     * <p>拦截器的清理方法，用于释放ThreadLocal中的用户信息
     * 
     * <p>调用时机：
     * <ul>
     *     <li>在整个MVC处理流程完成之后</li>
     *     <li>在视图渲染完成后，响应发送给客户端之后</li>
     *     <li>无论请求成功还是失败都会调用（finally语义）</li>
     * </ul>
     * 
     * <p>重要性：
     * <ul>
     *     <li>防止ThreadLocal内存泄漏（Tomcat使用线程池复用线程）</li>
     *     <li>避免用户信息被下一个请求错误读取</li>
     *     <li>保证线程安全和数据隔离</li>
     * </ul>
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器对象（Controller方法）
     * @param ex 处理过程中抛出的异常，如果没有异常则为null
     * @throws Exception 清理过程中可能抛出的异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 从ThreadLocal中移除当前请求的用户信息
        // 这是非常重要的清理操作，防止内存泄漏和数据混乱
        // 因为Web容器（如Tomcat）使用线程池，线程会被复用
        // 如果不清理，下一个请求复用同一线程时可能会读取到上一个用户的信息
        UserUtils.removeUser();
    }
}
