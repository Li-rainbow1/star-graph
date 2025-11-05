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

/**
 * Freemarker模板服务实现
 */
@Service
public class FreemarkerServiceImpl implements FreemarkerService {
    @Autowired
    Configuration configuration;

    /**
     * 渲染文生图工作流JSON，将参数填充到t2i.ftlh模板
     */
    @Override
    public String renderText2Image(ComfyuiModel comfyuiModel) throws Exception {
        Template template = configuration.getTemplate("t2i.ftlh");
        Map<String, Object> data = new HashMap<>();
        data.put("config", comfyuiModel);
        StringWriter out= new StringWriter();
        template.process(data, out);
        return out.toString();
    }
}
