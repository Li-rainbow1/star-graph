package cn.itcast.star.graph.core.dto.common;

import lombok.Data;

/**
 * 分页请求DTO
 * 
 * <p>用于接收分页查询的请求参数，包含页码和每页数量
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class PageRequestDto {

    /**
     * 页码，从1开始
     */
    private Integer pageNum;
    
    /**
     * 每页数量
     */
    private Integer pageSize;

    /**
     * 检查和修正分页参数
     * 
     * <p>确保分页参数在合理范围内：
     * <ul>
     *     <li>pageNum默认为1</li>
     *     <li>pageSize默认为150，最大不超过200</li>
     * </ul>
     */
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
