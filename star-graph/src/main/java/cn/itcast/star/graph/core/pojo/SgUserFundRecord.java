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
* sg_user_fund_record 实体类
* </p>
*
* @author luoxu
* @since 2024-10-18 17:48:19
*/
@Getter
@Setter
@TableName("sg_user_fund_record")
public class SgUserFundRecord implements Serializable {
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
    * 账户类型0积分账户，1积分冻结账户
    */
    private Integer fundType;

    /**
    * 额度
    */
    private Integer money;

    /**
    * 账户ID
    */
    private Long fundId;


}
