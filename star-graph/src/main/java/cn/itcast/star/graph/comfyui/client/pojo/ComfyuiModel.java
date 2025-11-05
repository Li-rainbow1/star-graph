package cn.itcast.star.graph.comfyui.client.pojo;

import lombok.Data;

/**
 * ComfyUI生图模型参数
 * 
 * @author itcast
 * @since 1.0
 */
@Data
public class ComfyuiModel {
    /** 随机种子，用于生成随机数，相同种子会生成相同图片 */
    private long seed;
    
    /** 采样步数，越多质量越好但耗时越长，一般20-50步 */
    private int step;
    
    /** CFG Scale，提示词引导强度，控制生成图片与提示词的相关度，一般7-12 */
    private int cfg;
    
    /** 采样器名称，如Euler、DPM++等 */
    private String samplerName;
    
    /** 调度器名称，控制去噪过程 */
    private String scheduler;
    
    /** 模型名称，指定使用的AI模型 */
    private String modelName;
    
    /** 生成图片宽度（像素） */
    private int width;
    
    /** 生成图片高度（像素） */
    private int height;
    
    /** 生成图片的数量 */
    private int size;
    
    /** 正向提示词，描述想要生成的内容 */
    private String propmt;
    
    /** 负向提示词，描述不想出现的内容 */
    private String reverse;
}
