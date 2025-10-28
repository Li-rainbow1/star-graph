package cn.itcast.star.graph.core.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * sg_user 实体类
 * </p>
 *
 * @author luoxu
 * @since 2024-10-15 14:44:00
 */
@Getter
@Setter
@TableName("sg_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除
     */
    private Integer deleted;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 手机
     */
    private String mobile;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 状态 0 正常  1超时锁定  2锁定  9无效
     */
    private Integer status;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 姓名
     */
    private String username;

    /**
     * 会员等级
     */
    private Integer vipLevel;
}
