package cn.itcast.star.graph;

import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiModel;
import cn.itcast.star.graph.core.service.FreemarkerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class FreemarkerServiceTest {
    @Autowired
    FreemarkerService freemarkerService;

    @Test
    public void test() throws Exception {
        ComfyuiModel model = new ComfyuiModel();
        model.setPropmt("a photo of an astronaut riding a horse on mars");
        model.setReverse("bad legs");
        model.setSamplerName("Euler");
        model.setScheduler("normal");
        model.setSize(1);
        model.setCfg(7);
        model.setWidth(512);
        model.setHeight(512);
        model.setStep(20);
        model.setSeed(0);
        model.setModelName("majicmixRealistic_v7.safetensors");
        String s = freemarkerService.renderText2Image(model);
        System.out.println(s);
    }
}
