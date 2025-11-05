package cn.itcast.star.graph.core.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.itcast.star.graph.core.dto.common.Result;
import cn.itcast.star.graph.core.dto.common.ResultCode;
import cn.itcast.star.graph.core.pojo.User;
import cn.itcast.star.graph.core.utils.JwtUtils;
import cn.itcast.star.graph.core.utils.UserUtils;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 用户认证拦截器
 * 
 * <p>从请求头解析JWT token，验证用户身份并存入ThreadLocal
 * 
 * @author itcast
 * @since 1.0
 */
public class UserInterceptor implements HandlerInterceptor {

    /**
     * 请求预处理，验证token并提取用户信息
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器对象
     * @return true-放行，false-拦截
     * @throws Exception 处理异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            String token = request.getHeader(HttpHeaders.AUTHORIZATION);
            
            if (StrUtil.isEmpty(token)){
                writeAuthorizationedFailed(response);
                return false;
            }
            
            User user = JwtUtils.getToken(token);
            
            if (user==null){
                writeAuthorizationedFailed(response);
                return false;
            }
            
            // 将用户信息存入ThreadLocal
            UserUtils.saveUser(user);
            return true;
        } catch (Exception e) {
            UserUtils.removeUser();
            throw e;
        }
    }

    /**
     * 返回401未授权响应
     * 
     * @param response HTTP响应对象
     */
    private void writeAuthorizationedFailed(HttpServletResponse response) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<Object> failed = Result.failed(ResultCode.ACCESS_UNAUTHORIZED);
        String jsonString = JSON.toJSONString(failed);
        
        try {
            PrintWriter writer = response.getWriter();
            writer.write(jsonString);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 请求完成后清理ThreadLocal，防止内存泄漏
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器对象
     * @param ex 异常对象
     * @throws Exception 处理异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserUtils.removeUser();
    }
}
