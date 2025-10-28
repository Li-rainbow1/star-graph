package cn.itcast.star.graph.core.dto.request;

import lombok.Data;

/**
 * 取消文生图任务请求DTO
 * 
 * <p>用于用户取消排队中的文生图任务。
 * 
 * <p><b>使用场景：</b>
 * <ul>
 *     <li>用户不想等待，主动取消排队任务</li>
 *     <li>取消后会自动归还冻结的积分</li>
 * </ul>
 * 
 * <p><b>限制：</b>
 * <ul>
 *     <li>只能取消排队中的任务</li>
 *     <li>已经开始执行的任务无法取消</li>
 *     <li>只能取消自己的任务</li>
 * </ul>
 * 
 * @author luoxu
 * @since 2024-10-24
 */
@Data
public class Text2ImageCancelReqDto {
    /**
     * 任务临时ID
     * 
     * <p>这是文生图接口返回的pid值，全局唯一标识一个任务
     * 
     * <p><b>注意：</b>不能为空或空字符串
     */
    private String tempId;
}
