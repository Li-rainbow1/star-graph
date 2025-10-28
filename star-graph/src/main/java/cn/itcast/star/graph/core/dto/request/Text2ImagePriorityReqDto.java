package cn.itcast.star.graph.core.dto.request;

import lombok.Data;

/**
 * 任务插队（提升优先级）请求DTO
 * 
 * <p>用于用户花费积分让排队中的任务提前执行。
 * 
 * <p><b>费用：</b>5积分/次
 * 
 * <p><b>使用场景：</b>
 * <ul>
 *     <li>任务排队时间较长，想要快速生成</li>
 *     <li>有足够的积分愿意支付插队费用</li>
 * </ul>
 * 
 * <p><b>限制：</b>
 * <ul>
 *     <li>只能对排队中的任务进行插队</li>
 *     <li>已经是第一名的任务无需插队</li>
 *     <li>已经开始执行的任务无法插队</li>
 *     <li>只能对自己的任务进行插队</li>
 *     <li>需要有足够的积分余额</li>
 * </ul>
 * 
 * <p><b>执行流程：</b>
 * <ol>
 *     <li>验证任务状态和权限</li>
 *     <li>检查是否已是第一名</li>
 *     <li>先提升优先级（修改Redis分值）</li>
 *     <li>再扣除积分（数据库事务）</li>
 *     <li>返回新的队列位置</li>
 * </ol>
 * 
 * @author luoxu
 * @since 2024-10-24
 */
@Data
public class Text2ImagePriorityReqDto {
    /**
     * 任务临时ID
     * 
     * <p>这是文生图接口返回的pid值，全局唯一标识一个任务
     * 
     * <p><b>注意：</b>不能为空或空字符串
     */
    private String tempId;
}
