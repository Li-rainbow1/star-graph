package cn.itcast.star.graph.core.service;

import cn.itcast.star.graph.core.pojo.UserResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户生图结果服务接口
 * 
 * <p>管理用户的文生图历史记录，针对表【sg_user_result(用户生图结果表)】的数据库操作Service
 * 
 * <p>主要功能：
 * <ul>
 *     <li>保存ComfyUI生成的图片URL到数据库</li>
 *     <li>扣除用户冻结的积分（图片生成成功后）</li>
 *     <li>提供用户生图历史查询</li>
 *     <li>支持图片收藏功能</li>
 * </ul>
 * 
 * <p>数据表字段：
 * <ul>
 *     <li>user_id - 用户ID</li>
 *     <li>url - 图片访问URL</li>
 *     <li>collect - 是否收藏（0-未收藏，1-已收藏）</li>
 *     <li>created_time - 创建时间</li>
 * </ul>
 * 
 * @author luoxu
 * @since 2024-10-18 16:07:13
 */
public interface UserResultService extends IService<UserResult> {

    /**
     * 批量保存图片URL并扣除积分
     * 
     * <p>此方法在任务执行成功后调用，执行两个关键操作：
     * <ol>
     *     <li>将生成的图片URL列表批量保存到数据库（用户历史记录）</li>
     *     <li>从用户冻结账户扣除积分到系统总账户（按图片数量扣除）</li>
     * </ol>
     * 
     * <p>事务保证：此方法需要在事务中执行，确保保存和扣除操作的原子性
     * 
     * @param urls 生成的图片URL列表，来自ComfyUI的executed消息
     * @param userId 用户ID，标识图片归属
     */
    public void saveList(List<String> urls, Long userId);

}