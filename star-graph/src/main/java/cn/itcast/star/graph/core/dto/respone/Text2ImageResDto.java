package cn.itcast.star.graph.core.dto.respone;

import lombok.Data;

/**
 * 文生图响应DTO
 * 
 * <p>用户提交文生图任务后的响应结果
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class Text2ImageResDto {

    /**
     * 任务ID（临时ID）
     * <p>用于后续取消、插队等操作
     */
    private String pid;
    
    /**
     * 队列序号
     * <p>表示当前任务在队列中的位置，0表示第一位
     */
    private long queueIndex = 0;

}