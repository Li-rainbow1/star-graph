package cn.itcast.star.graph.core.controller;

import cn.itcast.star.graph.core.dto.common.PageResult;
import cn.itcast.star.graph.core.dto.common.Result;
import cn.itcast.star.graph.core.dto.request.Text2ImageCancelReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImageListReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImagePriorityReqDto;
import cn.itcast.star.graph.core.dto.request.Text2ImageReqDto;
import cn.itcast.star.graph.core.dto.respone.Text2ImageResDto;
import cn.itcast.star.graph.core.pojo.UserResult;
import cn.itcast.star.graph.core.service.Text2ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文生图控制器
 * 
 * <p>提供文生图相关的HTTP接口，包括：
 * <ul>
 *     <li>创建文生图任务</li>
 *     <li>取消文生图任务</li>
 *     <li>查询历史记录</li>
 *     <li>任务插队（提升优先级）</li>
 * </ul>
 * 
 * <p>所有接口都需要用户认证，路径前缀：/api/authed/1.0/t2i
 * 
 * @author luoxu
 * @since 2024-10-24
 */
@RestController
@RequestMapping("/api/authed/1.0/t2i")
public class TextToImageController {
    
    @Autowired
    private Text2ImageService text2ImageService;

    /**
     * 创建文生图任务
     * 
     * <p>用户提交文生图请求后，系统会：
     * <ol>
     *     <li>冻结用户积分（根据生成图片数量）</li>
     *     <li>将任务加入Redis队列</li>
     *     <li>返回任务ID和队列位置</li>
     * </ol>
     * 
     * @param text2ImageReqDto 文生图请求参数，包含提示词、模型参数等
     * @return 包含任务ID(pid)和队列序号(queueIndex)的响应
     * @throws Exception 当积分不足或参数错误时抛出异常
     */
    @PostMapping("/propmt")
    public Result<Text2ImageResDto> propmt(@RequestBody Text2ImageReqDto text2ImageReqDto) throws Exception {
        Text2ImageResDto text2ImageResDto = text2ImageService.textToImage(text2ImageReqDto);
        return Result.ok(text2ImageResDto);
    }

    /**
     * 取消文生图任务
     * 
     * <p>用户可以取消排队中的任务，系统会：
     * <ol>
     *     <li>验证任务所属权限</li>
     *     <li>从Redis队列中删除任务</li>
     *     <li>归还冻结的积分</li>
     * </ol>
     * 
     * <p><b>注意：</b>只能取消还在队列中的任务，已经开始执行的任务无法取消
     * 
     * @param cancelReqDto 取消请求参数，包含任务ID
     * @return 成功返回空结果
     * @throws Exception 当任务已开始、不存在或无权限时抛出异常
     */
    @PostMapping("/canel")
    public Result<Void> cancel(@RequestBody Text2ImageCancelReqDto cancelReqDto) throws Exception {
        text2ImageService.cancelTask(cancelReqDto);
        return Result.ok();
    }

    /**
     * 查询用户的文生图历史记录
     * 
     * <p>分页查询当前用户的所有文生图记录，按创建时间倒序排列
     * 
     * @param listReqDto 分页请求参数，包含页码(pageNum)和每页数量(pageSize)
     * @return 分页结果，包含总数和记录列表
     */
    @PostMapping("/list")
    public Result<PageResult<List<UserResult>>> list(@RequestBody Text2ImageListReqDto listReqDto) {
        PageResult<List<UserResult>> pageResult = text2ImageService.getUserImageList(listReqDto);
        return Result.ok(pageResult);
    }

    /**
     * 任务插队（提升优先级）
     * 
     * <p>用户可以花费积分让自己的任务提前执行，系统会：
     * <ol>
     *     <li>验证任务状态和权限</li>
     *     <li>检查是否已是第一名</li>
     *     <li>扣除插队费用（5积分）</li>
     *     <li>提升队列优先级</li>
     *     <li>返回新的队列位置</li>
     * </ol>
     * 
     * <p><b>费用：</b>5积分/次
     * <p><b>限制：</b>只能对排队中的任务进行插队，已是第一名的任务无需插队
     * 
     * @param priorityReqDto 插队请求参数，包含任务ID
     * @return 插队后的新队列序号
     * @throws Exception 当任务已开始、已是第一名、积分不足或无权限时抛出异常
     */
    @PostMapping("/proprity")
    public Result<Long> priority(@RequestBody Text2ImagePriorityReqDto priorityReqDto) throws Exception {
        Long newRank = text2ImageService.increasePriority(priorityReqDto);
        return Result.ok(newRank);
    }

    /**
     * 获取任务的实时排名
     * 
     * <p>用户可以通过此接口查询任务的当前排队位置，系统会：
     * <ol>
     *     <li>验证任务所属权限</li>
     *     <li>计算正在执行的任务数</li>
     *     <li>获取任务在队列中的实际排名</li>
     *     <li>返回综合序号（执行数+排名）</li>
     * </ol>
     * 
     * <p><b>使用场景：</b>前端定期轮询此接口以实时更新用户看到的排队位置
     * <p><b>返回值说明：</b>
     * <ul>
     *     <li>返回null：任务已完成或被取消</li>
     *     <li>返回数字：当前排队位置（包含正在执行的任务）</li>
     * </ul>
     * 
     * @param priorityReqDto 包含任务ID的请求参数
     * @return 当前排队序号，null表示任务不在队列中
     * @throws Exception 当任务不存在或无权限时抛出异常
     */
    @PostMapping("/rank")
    public Result<Long> getTaskRank(@RequestBody Text2ImagePriorityReqDto priorityReqDto) throws Exception {
        Long rank = text2ImageService.getTaskRank(priorityReqDto);
        return Result.ok(rank);
    }
}