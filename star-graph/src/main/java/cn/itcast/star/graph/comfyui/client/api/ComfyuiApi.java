package cn.itcast.star.graph.comfyui.client.api;

import cn.itcast.star.graph.comfyui.client.pojo.DeleteQueueBody;
import cn.itcast.star.graph.comfyui.client.pojo.ComfyuiRequestDto;
import cn.itcast.star.graph.comfyui.client.pojo.QueueTaskCount;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.HashMap;

/**
 * ComfyUI HTTP API接口 - 使用Retrofit定义ComfyUI的REST API
 */
public interface ComfyuiApi {

    /**
     * 获取历史任务
     * @param maxItems 获取的条数
     */
    @GET("/history")
    Call<HashMap> getHistoryTasks(@Query("max_items") int maxItems);

    /**
     * 获取预览的图片信息
     * @param filename 文件名
     * @param type 文件类型(input/output)
     * @param subfolder 子文件夹名
     */
    @GET("/view")
    Call<ResponseBody> getView(@Query("filename") String filename, @Query("type") String type, @Query("subfolder") String subfolder);

    /**
     * 获取系统信息
     */
    @GET("/system_stats")
    Call<HashMap> getSystemStats();

    /**
     * 获取节点配置
     * @param nodeName 节点名称
     */
    @GET("/object_info/{nodeName}")
    Call<HashMap> getNodeInfo(@Path("nodeName") String nodeName);

    /**
     * 中断当前正在执行的任务
     * 注意：此接口返回空响应体（Content-Length: 0），因此返回类型为Void
     */
    @POST("/interrupt")
    Call<Void> interruptTask();

    /**
     * 获取队列任务信息
     */
    @GET("/queue")
    Call<HashMap> getQueueTasks();

    /**
     * 删除队列任务
     * @param body 删除请求体
     */
    @POST("/queue")
    Call<HashMap> deleteQueueTasks(@Body DeleteQueueBody body);

    /**
     * 获取队列任务数量
     */
    @GET("/prompt")
    Call<QueueTaskCount> getQueueTaskCount();

    /**
     * 添加流程任务
     * @param body 任务请求体
     */
    @POST("/prompt")
    Call<HashMap> addQueueTask(@Body ComfyuiRequestDto body);

    /**
     * 上传图片
     * @param image 图片文件
     */
    @Multipart
    @POST("/upload/image")
    Call<HashMap> uploadImage(@Part MultipartBody.Part image);

    /**
     * 上传蒙版图片
     */
    @Multipart
    @POST("/upload/mask")
    Call<HashMap> uploadMask(@Part MultipartBody.Part image,@Part("type") RequestBody type,@Part("subfolder") RequestBody subfolder,@Part("original_ref") RequestBody originalRef);

    /**
     * 获取指定历史任务
     * @param promptId 任务ID
     */
    @GET("/history/{promptId}")
    Call<HashMap> getHistoryTask(@Path("promptId") String promptId);

}
