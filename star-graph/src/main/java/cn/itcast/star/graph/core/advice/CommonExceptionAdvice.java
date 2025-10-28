// 定义包路径
package cn.itcast.star.graph.core.advice;

// 导入统一响应结果类，用于封装返回给前端的数据
import cn.itcast.star.graph.core.dto.common.Result;
// 导入自定义业务异常类，用于业务逻辑中的异常处理
import cn.itcast.star.graph.core.exception.CustomException;
// 导入Spring的HTTP状态码枚举类
import org.springframework.http.HttpStatus;
// 导入Spring的响应实体类，用于封装HTTP响应
import org.springframework.http.ResponseEntity;
// 导入Spring的异常处理器注解，用于标识异常处理方法
import org.springframework.web.bind.annotation.ExceptionHandler;
// 导入Spring的全局异常处理注解，拦截所有@RestController中的异常
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器（Global Exception Handler）
 * 
 * <p>统一处理应用中抛出的各类异常，返回友好的错误信息给前端
 * 
 * <p>主要功能：
 * <ul>
 *     <li>捕获Controller层抛出的各类异常</li>
 *     <li>将异常信息转换为统一的JSON响应格式</li>
 *     <li>避免异常信息直接暴露给前端用户</li>
 *     <li>提供友好的错误提示</li>
 * </ul>
 * 
 * <p>工作原理：
 * <ul>
 *     <li>@RestControllerAdvice：Spring AOP切面，拦截所有@RestController</li>
 *     <li>@ExceptionHandler：指定要处理的异常类型</li>
 *     <li>当匹配的异常抛出时，Spring自动调用对应的处理方法</li>
 *     <li>返回统一格式的错误响应给前端</li>
 * </ul>
 * 
 * <p>异常处理流程：
 * <ol>
 *     <li>Controller方法执行过程中抛出异常</li>
 *     <li>Spring MVC拦截异常，查找匹配的@ExceptionHandler</li>
 *     <li>调用异常处理方法，构造错误响应</li>
 *     <li>返回JSON格式的错误信息给前端</li>
 * </ol>
 * 
 * <p>优点：
 * <ul>
 *     <li>统一异常处理逻辑，避免在每个Controller中重复编写</li>
 *     <li>提供一致的错误响应格式</li>
 *     <li>增强系统的健壮性和用户体验</li>
 *     <li>便于日志记录和问题排查</li>
 * </ul>
 * 
 * <p>响应格式示例：
 * <pre>
 * {
 *   "code": 500,
 *   "message": "任务ID不能为空",
 *   "data": null
 * }
 * </pre>
 * 
 * @author itcast
 * @since 1.0
 */
// 标识这是一个全局异常处理类
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody
// 作用：拦截所有@RestController中抛出的异常，并将处理结果自动转换为JSON返回
@RestControllerAdvice
// 全局异常处理器类
public class CommonExceptionAdvice {

    /**
     * 处理自定义业务异常（CustomException）
     * 
     * <p>捕获业务逻辑中抛出的CustomException异常，返回包含错误信息的响应
     * 
     * <p>CustomException使用场景：
     * <ul>
     *     <li>参数校验失败（如：任务ID不能为空）</li>
     *     <li>业务规则违反（如：任务已经是第一名，无需插队）</li>
     *     <li>资源不存在（如：任务不存在）</li>
     *     <li>权限不足（如：无权限操作该任务）</li>
     *     <li>状态异常（如：任务已经开始或不存在）</li>
     * </ul>
     * 
     * <p>处理策略：
     * <ul>
     *     <li>HTTP状态码：200（业务异常，非HTTP异常）</li>
     *     <li>响应体：Result对象，code为错误码，message为异常消息</li>
     *     <li>不记录堆栈信息（业务异常是预期内的，无需堆栈）</li>
     * </ul>
     * 
     * <p>与系统异常的区别：
     * <ul>
     *     <li>CustomException：业务逻辑异常，返回200+错误消息</li>
     *     <li>系统异常（如NullPointerException）：返回500+通用错误消息</li>
     * </ul>
     * 
     * @param customException 自定义业务异常对象，包含错误消息
     * @return ResponseEntity包装的Result对象，HTTP状态码200，body为错误信息
     */
    // 指定此方法处理CustomException类型的异常
    // 当Controller抛出CustomException时，Spring会自动调用此方法
    @ExceptionHandler(CustomException.class)
    public ResponseEntity customException(CustomException customException){
        // 构建响应实体（ResponseEntity）
        // ResponseEntity.status()：设置HTTP状态码
        // HttpStatus.OK.value()：200状态码（业务异常返回200，表示HTTP请求成功，但业务失败）
        // .body()：设置响应体内容
        // Result.error()：构造错误响应对象，包含错误消息
        // customException.getMessage()：获取异常中的错误消息（如："任务ID不能为空"）
        ResponseEntity<Result<Object>> responseEntity = ResponseEntity.status(HttpStatus.OK.value())
                .body(Result.error(customException.getMessage()));
        
        // 返回响应实体给前端
        // 前端会收到：{"code": 500, "message": "xxx", "data": null}
        return responseEntity;
    }
    
    // 【可扩展】可以添加更多异常处理方法
    // 例如：
    // @ExceptionHandler(Exception.class) - 处理所有未被捕获的异常
    // @ExceptionHandler(MethodArgumentNotValidException.class) - 处理参数校验异常
    // @ExceptionHandler(SQLException.class) - 处理数据库异常
}
