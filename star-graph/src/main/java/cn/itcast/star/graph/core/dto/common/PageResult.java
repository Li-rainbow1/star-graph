package cn.itcast.star.graph.core.dto.common;

import cn.hutool.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页响应结果封装类
 * 
 * <p>继承自Result，额外增加总数量字段，用于分页查询的响应
 * 
 * @param <T> 响应数据的类型
 * @author itcast
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> extends Result<T> {
    /**
     * 总记录数
     */
    private long total;

    /**
     * 返回分页成功结果
     * 
     * @param total 总记录数
     * @param data 当前页数据
     * @return 分页响应结果
     */
    public static PageResult ok(long total, Object data) {
        PageResult tPageResult = new PageResult<>();
        tPageResult.setCode(HttpStatus.HTTP_OK+"");
        tPageResult.setMsg(REQUEST_OK);
        tPageResult.setTotal(total);
        tPageResult.setData(data);
        return tPageResult;
    }

}
