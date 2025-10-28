package cn.itcast.star.graph.core.service;

import cn.itcast.star.graph.core.pojo.SgUserFundRecord;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserFundRecordService extends IService<SgUserFundRecord> {
    // 积分冻结
    void pointsFreeze(Long userId, Integer money);
    // 冻结归还
    void freezeReturn(Long userId, Integer money);
    // 积分扣除
    void pointsDeduction(Long userId, Integer money);
    // 直接划扣
    void directDeduction(Long userId, Integer money);
    // 直接归还（用于直接划扣失败后的补偿）
    void directRefund(Long userId, Integer money);
}