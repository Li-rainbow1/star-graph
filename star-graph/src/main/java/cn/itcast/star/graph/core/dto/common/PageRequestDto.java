package cn.itcast.star.graph.core.dto.common;

import lombok.Data;

/**
 * 分页请求DTO
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class PageRequestDto {

    /** 页码 */
    private Integer pageNum;
    
    /** 每页数量 */
    private Integer pageSize;

    /** 检查和修正分页参数 */
    public void checkPage()
    {
        if(pageNum==null||pageNum<=0)
        {
            pageNum = 1;
        }
        if(pageSize==null||pageSize<=0)
        {
            pageSize = 150;
        }
        if(pageSize>200){
            pageSize = 150;
        }
    }
}
