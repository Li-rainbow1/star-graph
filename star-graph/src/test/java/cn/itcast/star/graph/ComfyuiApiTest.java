package cn.itcast.star.graph;

import cn.itcast.star.graph.comfyui.client.api.ComfyuiApi;
import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiRequestDto;
import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.HashMap;

@SpringBootTest
public class ComfyuiApiTest {
    @Autowired
    ComfyuiApi comfyuiApi;

    @Test
    public void test() throws IOException {
        Call<HashMap> systemStats = comfyuiApi.getSystemStats();
        Response<HashMap> execute = systemStats.execute();
        System.out.println(JSON.toJSONString(execute.body()));
    }

    @Test
    public void testAdd() throws IOException {
        String json = "{\n" +
                "  \"3\": {\n" +
                "    \"inputs\": {\n" +
                "      \"seed\": 220540804925548,\n" +
                "      \"steps\": 20,\n" +
                "      \"cfg\": 8,\n" +
                "      \"sampler_name\": \"dpmpp_3m_sde\",\n" +
                "      \"scheduler\": \"karras\",\n" +
                "      \"denoise\": 1,\n" +
                "      \"model\": [\n" +
                "        \"4\",\n" +
                "        0\n" +
                "      ],\n" +
                "      \"positive\": [\n" +
                "        \"6\",\n" +
                "        0\n" +
                "      ],\n" +
                "      \"negative\": [\n" +
                "        \"7\",\n" +
                "        0\n" +
                "      ],\n" +
                "      \"latent_image\": [\n" +
                "        \"15\",\n" +
                "        0\n" +
                "      ]\n" +
                "    },\n" +
                "    \"class_type\": \"KSampler\",\n" +
                "    \"_meta\": {\n" +
                "      \"title\": \"K采样器\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"4\": {\n" +
                "    \"inputs\": {\n" +
                "      \"ckpt_name\": \"majicmixRealistic_v7.safetensors\"\n" +
                "    },\n" +
                "    \"class_type\": \"CheckpointLoaderSimple\",\n" +
                "    \"_meta\": {\n" +
                "      \"title\": \"Checkpoint加载器(简易)\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"6\": {\n" +
                "    \"inputs\": {\n" +
                "      \"text\": \"beauty girl,8k,Facial details\",\n" +
                "      \"clip\": [\n" +
                "        \"4\",\n" +
                "        1\n" +
                "      ]\n" +
                "    },\n" +
                "    \"class_type\": \"CLIPTextEncode\",\n" +
                "    \"_meta\": {\n" +
                "      \"title\": \"CLIP文本编码器\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"7\": {\n" +
                "    \"inputs\": {\n" +
                "      \"text\": \"bad fingers,bad hands,bad legs\",\n" +
                "      \"clip\": [\n" +
                "        \"4\",\n" +
                "        1\n" +
                "      ]\n" +
                "    },\n" +
                "    \"class_type\": \"CLIPTextEncode\",\n" +
                "    \"_meta\": {\n" +
                "      \"title\": \"CLIP文本编码器\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"8\": {\n" +
                "    \"inputs\": {\n" +
                "      \"samples\": [\n" +
                "        \"3\",\n" +
                "        0\n" +
                "      ],\n" +
                "      \"vae\": [\n" +
                "        \"4\",\n" +
                "        2\n" +
                "      ]\n" +
                "    },\n" +
                "    \"class_type\": \"VAEDecode\",\n" +
                "    \"_meta\": {\n" +
                "      \"title\": \"VAE解码\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"12\": {\n" +
                "    \"inputs\": {\n" +
                "      \"images\": [\n" +
                "        \"8\",\n" +
                "        0\n" +
                "      ]\n" +
                "    },\n" +
                "    \"class_type\": \"PreviewImage\",\n" +
                "    \"_meta\": {\n" +
                "      \"title\": \"预览图像\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"15\": {\n" +
                "    \"inputs\": {\n" +
                "      \"grow_mask_by\": 12,\n" +
                "      \"pixels\": [\n" +
                "        \"18\",\n" +
                "        0\n" +
                "      ],\n" +
                "      \"vae\": [\n" +
                "        \"4\",\n" +
                "        2\n" +
                "      ],\n" +
                "      \"mask\": [\n" +
                "        \"18\",\n" +
                "        1\n" +
                "      ]\n" +
                "    },\n" +
                "    \"class_type\": \"VAEEncodeForInpaint\",\n" +
                "    \"_meta\": {\n" +
                "      \"title\": \"VAE内补编码器\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"18\": {\n" +
                "    \"inputs\": {\n" +
                "      \"image\": \"clipspace/clipspace-mask-5771519.400000002.png [input]\",\n" +
                "      \"upload\": \"image\"\n" +
                "    },\n" +
                "    \"class_type\": \"LoadImage\",\n" +
                "    \"_meta\": {\n" +
                "      \"title\": \"加载图像\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        ComfyuiRequestDto comfyuiRequestDto = new ComfyuiRequestDto("123456", JSON.parseObject(json));
        Call<HashMap> hashMapCall = comfyuiApi.addQueueTask(comfyuiRequestDto);
        Response<HashMap> execute = hashMapCall.execute();
        System.out.println(JSON.toJSONString(execute.body()));
    }

}
