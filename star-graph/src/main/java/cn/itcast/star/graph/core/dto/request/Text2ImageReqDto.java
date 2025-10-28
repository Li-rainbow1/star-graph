package cn.itcast.star.graph.core.dto.request;

import lombok.Data;

/**
 * 文生图请求DTO
 * 
 * <p>用户提交文生图任务的请求参数，包含各项生图配置
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class Text2ImageReqDto {
    /** 生成图片数量 */
    int size;
    
    /** 模型类型：1-majicmixRealistic, 2-anythingelseV4 */
    int model;
    
    /** 图片尺寸：1-512x512, 2-512x768, 3-768x512 */
    int scale;
    
    /** 采样步数，控制生成质量，一般20-50步 */
    int step;
    
    /** CFG Scale，提示词引导强度，一般7-12 */
    int cfg;
    
    /** 采样器类型：1-dpmpp_sde, 2-dpmpp_2m, 3-euler, 4-dpmpp_3m_sde */
    int sampler;
    
    /** 随机种子，相同种子生成相同图片 */
    int seed;
    
    /** 负向提示词，描述不想出现的内容 */
    String reverse;
    
    /** 正向提示词，描述想要生成的内容 */
    String propmt;
    
    /** 客户端ID */
    String clientId;

    /**
     * 获取模型名称
     * 
     * @return 模型文件名
     */
    public String modelName(){
        switch (model){
            case 2:
                return "anythingelseV4_v45.safetensors";
            default:
                return "majicmixRealistic_v7.safetensors";
        }
    }

    /**
     * 获取采样器名称
     * 
     * @return 采样器名称
     */
    public String samplerName(){
        switch (sampler){
            case 1:
                return "dpmpp_sde";
            case 2:
                return "dpmpp_2m";
            case 3:
                return "euler";
            case 4:
                return "dpmpp_3m_sde";
            default:
                return "euler";
        }
    }

    /**
     * 获取调度器名称
     * 
     * @return 调度器名称
     */
    public String scheduler(){
        return "karras";
    }


    /**
     * 计算图片宽度
     * 
     * @return 图片宽度（像素）
     */
    public int width() {
        if(scale==3){
            return 768;
        } else {
            return 512;
        }
    }

    /**
     * 计算图片高度
     * 
     * @return 图片高度（像素）
     */
    public int height() {
        if(scale==2){
            return 768;
        } else {
            return 512;
        }
    }
}
