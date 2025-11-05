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
 * 文生图控制器 - 提供文生图任务的创建、取消、查询、插队等接口
 */
@RestController
@RequestMapping("/api/authed/1.0/t2i")
public class TextToImageController {
    
    @Autowired
    private Text2ImageService text2ImageService;

    /**
     * 创建文生图任务，返回任务ID和队列序号
     */
    @PostMapping("/propmt")
    public Result<Text2ImageResDto> propmt(@RequestBody Text2ImageReqDto text2ImageReqDto) throws Exception {
        Text2ImageResDto text2ImageResDto = text2ImageService.textToImage(text2ImageReqDto);
        return Result.ok(text2ImageResDto);
    }

    /**
     * 取消任务（智能取消：队列中的任务直接删除并退款，正在执行的任务调用中断接口）
     */
    @PostMapping("/canel")
    public Result<Void> cancel(@RequestBody Text2ImageCancelReqDto cancelReqDto) throws Exception {
        text2ImageService.cancelTask(cancelReqDto);
        return Result.ok();
    }

    /**
     * 查询用户的文生图历史记录（分页）
     */
    @PostMapping("/list")
    public Result<PageResult<List<UserResult>>> list(@RequestBody Text2ImageListReqDto listReqDto) {
        PageResult<List<UserResult>> pageResult = text2ImageService.getUserImageList(listReqDto);
        return Result.ok(pageResult);
    }

    /**
     * 任务插队，费用5积分/次
     */
    @PostMapping("/proprity")
    public Result<Long> priority(@RequestBody Text2ImagePriorityReqDto priorityReqDto) throws Exception {
        Long newRank = text2ImageService.increasePriority(priorityReqDto);
        return Result.ok(newRank);
    }

    /**
     * 获取任务的实时排名（用于前端轮询）
     */
    @PostMapping("/rank")
    public Result<Long> getTaskRank(@RequestBody Text2ImagePriorityReqDto priorityReqDto) throws Exception {
        Long rank = text2ImageService.getTaskRank(priorityReqDto);
        return Result.ok(rank);
    }
}