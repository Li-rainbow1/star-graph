package cn.itcast.star.graph.core.pojo;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
* <p>
* sg_user_fund 实体类
* </p>
*
* @author luoxu
* @since 2024-10-18 17:48:19
*/
@Getter
@Setter
@TableName("sg_user_fund")
public class SgUserFund implements Serializable {
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
    * 积分账户
    */
    private Long score;

    /**
    * 乐观锁
    */
    @Version
    private Long version;

    /**
    * 积分冻结账户
    */
    private Long freezeScore;

    /**
    * 用户ID
    */
    private Long userId;


}
