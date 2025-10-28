package cn.itcast.star.graph.core.service.impl;

import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiModel;
import cn.itcast.star.graph.core.service.FreemarkerService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
public class FreemarkerServiceImpl implements FreemarkerService {
    @Autowired
    Configuration configuration;

    @Override
    public String renderText2Image(ComfyuiModel comfyuiModel) throws Exception {
        // 从Freemarker配置中加载文生图模板文件（t2i.ftlh）
        Template template = configuration.getTemplate("t2i.ftlh");
        // 创建数据模型Map，用于向模板传递数据
        Map<String, Object> data = new HashMap<>();
        // 将ComfyUI模型参数对象放入数据模型，键名为"config"
        data.put("config", comfyuiModel);
        // 创建字符串输出流，用于接收模板渲染后的结果
        StringWriter out= new StringWriter();
        // 处理模板：
        // 第一个参数：模板要使用的数据模型（包含config对象）
        // 第二个参数：模板渲染结果的输出目标（StringWriter）
        // 模板引擎会将模板中的占位符替换为实际数据，生成ComfyUI工作流JSON
        template.process(data, out);

        // 将StringWriter转换为字符串并返回（即生成的ComfyUI工作流JSON字符串）
        return out.toString();
    }
}
