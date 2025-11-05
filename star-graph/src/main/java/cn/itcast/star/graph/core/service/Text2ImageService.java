package cn.itcast.star.graph.core.service;

import cn.itcast.star.graph.core.dto.common.PageResult;
import cn.itcast.star.graph.core.dto.request.Text2ImageCancelReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImageListReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImagePriorityReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImageReqDto;
import cn.itcast.star.graph.core.dto.respone.Text2ImageResDto;
import cn.itcast.star.graph.core.pojo.UserResult;

import java.util.List;

/**
 * 文生图服务接口
 * 
 * <p>处理文生图相关的核心业务逻辑，包括：
 * <ul>
 *     <li>创建文生图任务</li>
 *     <li>取消任务</li>
 *     <li>查询历史记录</li>
 *     <li>任务插队（提升优先级）</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
public interface Text2ImageService {
    /**
     * 创建文生图任务
     * 
     * @param text2ImageReqDto 文生图请求参数
     * @return 任务响应，包含任务ID和队列位置
     * @throws Exception 当积分不足或参数错误时
     */
    Text2ImageResDto textToImage(Text2ImageReqDto text2ImageReqDto) throws Exception;
    
    /**
     * 取消文生图任务（智能取消）
     * 
     * <p>根据任务状态自动选择取消方式：
     * <ul>
     *     <li>队列中的任务：从队列删除并归还冻结积分</li>
     *     <li>正在执行的任务：调用ComfyUI中断接口</li>
     * </ul>
     * 
     * @param cancelReqDto 取消请求参数
     * @throws Exception 当任务不存在、已完成或无权限时
     */
    void cancelTask(Text2ImageCancelReqDto cancelReqDto) throws Exception;
    
    /**
     * 查询用户的文生图历史记录
     * 
     * @param listReqDto 分页请求参数
     * @return 分页结果
     */
    PageResult<List<UserResult>> getUserImageList(Text2ImageListReqDto listReqDto);
    
    /**
     * 任务插队（提升优先级）
     * 
     * @param priorityReqDto 插队请求参数
     * @return 插队后的新队列位置
     * @throws Exception 当任务已开始、积分不足或无权限时
     */
    Long increasePriority(Text2ImagePriorityReqDto priorityReqDto) throws Exception;
    
    /**
     * 获取任务的实时排名
     * 
     * <p>查询任务在队列中的当前位置，包含正在执行的任务数
     * 
     * @param priorityReqDto 包含任务ID的请求参数
     * @return 当前排队序号，null表示任务已完成或被取消
     * @throws Exception 当任务不存在或无权限时
     */
    Long getTaskRank(Text2ImagePriorityReqDto priorityReqDto) throws Exception;
}
