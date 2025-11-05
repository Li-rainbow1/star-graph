package cn.itcast.star.graph.core.advice;

import cn.itcast.star.graph.core.dto.common.Result;
import cn.itcast.star.graph.core.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 - 统一处理Controller层异常
 */
@RestControllerAdvice
public class CommonExceptionAdvice {

    /**
     * 捕获自定义业务异常，返回错误信息
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity customException(CustomException customException){
        ResponseEntity<Result<Object>> responseEntity = ResponseEntity.status(HttpStatus.OK.value())
                .body(Result.error(customException.getMessage()));
        return responseEntity;
    }
}
