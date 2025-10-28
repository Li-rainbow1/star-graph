package cn.itcast.star.graph.core.service;

import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiModel;

import java.io.IOException;

/**
 * Freemarker模板服务接口
 * 
 * <p>使用Freemarker模板引擎将参数渲染为ComfyUI工作流JSON
 * 
 * <p>Freemarker模板的作用：
 * <ul>
 *     <li>将用户的参数（模型、尺寸、步数、提示词等）填充到预定义的工作流模板</li>
 *     <li>生成ComfyUI可以识别的标准工作流JSON格式</li>
 *     <li>支持不同的生图模式（文生图、图生图等）</li>
 * </ul>
 * 
 * <p>工作流JSON包含：
 * <ul>
 *     <li>节点配置（加载模型、采样器、编码器等）</li>
 *     <li>节点连接关系（数据流向）</li>
 *     <li>参数设置（提示词、尺寸、步数等）</li>
 * </ul>
 * 
 * @author itcast
 * @since 1.0
 */
public interface FreemarkerService {
    /**
     * 渲染文生图工作流JSON
     * 
     * <p>将ComfyUI模型参数对象填充到Freemarker模板（t2i.ftlh），
     * 生成完整的ComfyUI工作流JSON字符串
     * 
     * <p>渲染过程：
     * <ol>
     *     <li>加载t2i.ftlh模板文件</li>
     *     <li>将comfyuiModel对象传入模板</li>
     *     <li>模板引擎替换占位符为实际参数值</li>
     *     <li>返回生成的JSON字符串</li>
     * </ol>
     * 
     * @param comfyuiModel ComfyUI模型参数对象，包含模型名、采样器、尺寸、提示词等
     * @return ComfyUI工作流JSON字符串，可直接提交给ComfyUI执行
     * @throws Exception 模板文件不存在或渲染失败时抛出
     */
    public String renderText2Image(ComfyuiModel comfyuiModel) throws Exception;
}
