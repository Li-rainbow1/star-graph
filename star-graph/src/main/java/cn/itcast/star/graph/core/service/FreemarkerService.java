package cn.itcast.star.graph.core.service;

import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiModel;

import java.io.IOException;

/**
 * Freemarker模板服务 - 渲染ComfyUI工作流JSON
 */
public interface FreemarkerService {
    /**
     * 将参数填充到模板，生成文生图工作流JSON
     */
    public String renderText2Image(ComfyuiModel comfyuiModel) throws Exception;
}
