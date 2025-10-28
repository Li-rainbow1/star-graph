package cn.itcast.star.graph.core.exception;

/**
 * 自定义业务异常类
 * 
 * <p>用于封装业务逻辑中的异常情况，如：
 * <ul>
 *     <li>积分不足</li>
 *     <li>任务不存在</li>
 *     <li>无权限操作</li>
 *     <li>参数验证失败</li>
 * </ul>
 * 
 * <p>抛出此异常后会被全局异常处理器捕获，返回友好的错误信息给前端
 * 
 * @author itcast
 * @since 1.0
 */
public class CustomException extends RuntimeException{
    /**
     * 构造自定义异常
     * 
     * @param msg 错误消息
     */
    public CustomException(String msg){
        super(msg);
    }
}
