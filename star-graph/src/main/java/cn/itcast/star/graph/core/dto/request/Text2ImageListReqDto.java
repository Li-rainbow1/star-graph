package cn.itcast.star.graph.core.dto.request;

import lombok.Data;

/**
 * 文生图历史列表请求DTO
 * 
 * <p>用于查询用户的文生图历史记录，支持分页
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class Text2ImageListReqDto {
    /**
     * 当前页，最小值为1
     */
    private Integer pageNum = 1;
    
    /**
     * 每页条数，取值范围为1~20
     */
    private Integer pageSize = 10;
}
