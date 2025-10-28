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
* sg_user_result 实体类
* </p>
*
* @author luoxu
* @since 2024-10-18 16:06:17
*/
@Getter
@Setter
@TableName("sg_user_result")
public class UserResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
    * 主键
    */
    @TableId
    private Long id;

    /**
    * 创建时间
    */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
    * 用户
    */
    private Long userId;

    /**
    * 是否收藏
    */
    private Integer collect;

    /**
    * 图片地址
    */
    private String url;


}
