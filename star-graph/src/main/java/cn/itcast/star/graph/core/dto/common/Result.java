package cn.itcast.star.graph.core.dto.common;

import cn.hutool.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用响应结果封装类
 * 
 * <p>统一封装所有HTTP接口的响应结果，包含：
 * <ul>
 *     <li>code：响应状态码</li>
 *     <li>msg：响应消息</li>
 *     <li>data：响应数据</li>
 * </ul>
 * 
 * @param <T> 响应数据的类型
 * @author itcast
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result<T> {
    /** 请求成功的消息 */
    protected static final String REQUEST_OK = "ok";
    
    /** 响应状态码 */
    private String code;
    
    /** 响应消息 */
    private String msg;
    
    /** 响应数据 */
    private T data;

    /**
     * 返回成功结果（无数据）
     * 
     * @return 成功响应
     */
    public static Result<Void> ok() {
        return new Result<Void>(HttpStatus.HTTP_OK+"", REQUEST_OK, null);
    }

    /**
     * 返回成功结果（包含数据）
     * 
     * @param body 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> Result<T> ok(T body) {
        return new Result(HttpStatus.HTTP_OK+"", REQUEST_OK, body);
    }

    /**
     * 返回错误结果
     * 
     * @param msg 错误消息
     * @param <T> 数据类型
     * @return 错误响应
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(HttpStatus.HTTP_BAD_REQUEST+"", msg, null);
    }

    /**
     * 返回错误结果（指定状态码）
     * 
     * @param code 状态码
     * @param msg 错误消息
     * @param <T> 数据类型
     * @return 错误响应
     */
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code+"", msg, null);
    }
    /**
     * 返回失败结果（使用结果码枚举）
     * 
     * @param resultCode 结果码枚举
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> Result<T> failed(ResultCode resultCode) {
        return  new Result<>(resultCode.getCode(), resultCode.getMsg(), null);
    }

}
