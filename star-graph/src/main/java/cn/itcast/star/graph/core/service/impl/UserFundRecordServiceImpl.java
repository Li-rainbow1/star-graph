package cn.itcast.star.graph.core.service.impl;

import cn.itcast.star.graph.core.exception.CustomException;
import cn.itcast.star.graph.core.mapper.SgUserFundMapper;
import cn.itcast.star.graph.core.mapper.SgUserFundRecordMapper;
import cn.itcast.star.graph.core.pojo.SgUserFund;
import cn.itcast.star.graph.core.pojo.SgUserFundRecord;
import cn.itcast.star.graph.core.service.UserFundRecordService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户积分记录服务实现 - 管理积分冻结、扣除、归还等操作
 */
@Service
@Transactional
@Slf4j
public class UserFundRecordServiceImpl extends ServiceImpl<SgUserFundRecordMapper, SgUserFundRecord> implements UserFundRecordService {

    @Autowired
    SgUserFundMapper sgUserFundMapper;
    @Autowired
    SgUserFundRecordMapper sgUserFundRecordMapper;
    
    /**
     * 积分冻结：从可用账户扣除，增加到冻结账户
     */
    @Override
    public void pointsFreeze(Long userId, Integer money) {
        // 获取用户的积分账户信息（如果不存在会自动创建）
        SgUserFund sgUserFund = getUserSgUserFund(userId);
        // 计算扣除积分后的余额
        long temp = sgUserFund.getScore() - money;
        // 判断可用积分是否足够
        if(temp>=0){
            // 减少可用积分
            sgUserFund.setScore(temp);
            // 增加冻结积分（冻结的积分暂时不可用，等任务完成后扣除或失败后归还）
            sgUserFund.setFreezeScore(sgUserFund.getFreezeScore()+money);
            // 更新数据库中的积分记录
            int updated = sgUserFundMapper.updateById(sgUserFund);
            // 如果更新失败（可能是并发冲突），抛出异常
            if (updated == 0) {
                throw new CustomException("积分更新失败，请重试");
            }

            // 记录积分变动日志：可用积分减少
            saveLog(0,-money,sgUserFund.getId());
            // 记录积分变动日志：冻结积分增加
            saveLog(1,money,sgUserFund.getId());
        }else{
            // 可用积分不足，无法冻结，抛出异常
            throw new CustomException("积分账户余额不足");
        }
    }

    /**
     * 冻结归还：从冻结账户扣除，增加到可用账户
     */
    @Override
    public void freezeReturn(Long userId, Integer money) {
        // 获取用户的积分账户信息
        SgUserFund sgUserFund = getUserSgUserFund(userId);
        // 计算归还积分后的冻结余额
        long temp = sgUserFund.getFreezeScore() - money;
        // 判断冻结积分是否足够归还
        if(temp>=0){
            // 增加可用积分（从冻结账户归还到可用账户）
            sgUserFund.setScore(sgUserFund.getScore()+money);
            // 减少冻结积分
            sgUserFund.setFreezeScore(temp);
            // 更新数据库中的积分记录
            int updated = sgUserFundMapper.updateById(sgUserFund);
            // 如果更新失败，抛出异常
            if (updated == 0) {
                throw new CustomException("积分更新失败，请重试");
            }

            // 记录积分变动日志：可用积分增加
            saveLog(0,money,sgUserFund.getId());
            // 记录积分变动日志：冻结积分减少
            saveLog(1,-money,sgUserFund.getId());
        }else{
            // 冻结积分不足，抛出异常（理论上不应该发生）
            throw new CustomException("积分冻结账户余额不足");
        }
    }

    /**
     * 积分扣除：从冻结账户扣除，增加到系统总账户（平台收入）
     */
    @Override
    public void pointsDeduction(Long userId, Integer money) {
        // 获取用户的积分账户信息
        SgUserFund userFund = getUserSgUserFund(userId);
        // 获取系统总账户信息（userId=0代表系统总账户）
        SgUserFund allFund = getUserSgUserFund(0);

        // 计算扣除积分后的冻结余额
        long temp = userFund.getFreezeScore() - money;
        // 判断冻结积分是否足够扣除
        if(temp>=0){
            // 第一步：从用户冻结账户扣除积分
            userFund.setFreezeScore(temp);
            // 记录用户冻结积分减少的日志
            saveLog(1,-money,userFund.getId());
            // 更新用户积分记录到数据库
            int updated1 = sgUserFundMapper.updateById(userFund);
            // 如果更新失败，抛出异常回滚事务
            if (updated1 == 0) {
                throw new CustomException("积分更新失败，请重试");
            }

            // 第二步：将扣除的积分增加到系统总账户（平台收入）
            allFund.setScore(allFund.getScore()+money);
            // 记录系统总账户积分增加的日志
            saveLog(0,money,allFund.getId());
            // 更新系统总账户记录到数据库
            int updated2 = sgUserFundMapper.updateById(allFund);
            // 如果更新失败，抛出异常回滚事务
            if (updated2 == 0) {
                throw new CustomException("积分更新失败，请重试");
            }
        }else{
            // 冻结积分不足，无法扣除，抛出异常
            throw new CustomException("积分冻结账户余额不足");
        }

    }

    /**
     * 直接划扣：从可用账户直接扣除，增加到系统总账户（用于插队等即时消费）
     */
    @Override
    public void directDeduction(Long userId, Integer money) {
        // 获取用户的积分账户信息
        SgUserFund userFund = getUserSgUserFund(userId);
        // 获取系统总账户信息（userId=0L代表系统总账户）
        SgUserFund allFund = getUserSgUserFund(0L);

        // 计算扣除积分后的可用余额
        long temp = userFund.getScore() - money;
        // 判断可用积分是否足够直接扣除
        if(temp >= 0){
            // 第一步：从用户可用积分中直接扣除（无需冻结，直接扣除，用于插队等场景）
            userFund.setScore(temp);
            // 记录用户可用积分减少的日志
            saveLog(0, -money, userFund.getId());
            // 更新用户积分记录到数据库
            int updated1 = sgUserFundMapper.updateById(userFund);
            // 如果更新失败，抛出异常回滚事务
            if (updated1 == 0) {
                throw new CustomException("积分更新失败，请重试");
            }

            // 第二步：将扣除的积分增加到系统总账户（平台收入）
            allFund.setScore(allFund.getScore() + money);
            // 记录系统总账户积分增加的日志
            saveLog(0, money, allFund.getId());
            // 更新系统总账户记录到数据库
            int updated2 = sgUserFundMapper.updateById(allFund);
            // 如果更新失败，抛出异常回滚事务
            if (updated2 == 0) {
                throw new CustomException("积分更新失败，请重试");
            }
        } else {
            // 可用积分不足，无法直接扣除，抛出异常
            throw new CustomException("账户积分不足");
        }
    }

    /**
     * 保存积分变动日志
     * 
     * @param fundType 资金类型：0-可用积分，1-冻结积分
     * @param money 变动金额（正数表示增加，负数表示减少）
     * @param fundId 资金账户ID
     */
    private void saveLog(int fundType,int money,long fundId){
        // 创建积分变动记录对象
        SgUserFundRecord log = new SgUserFundRecord();
        // 设置资金类型（0=可用积分，1=冻结积分）
        log.setFundType(fundType);
        // 设置变动金额
        log.setMoney(money);
        // 设置关联的资金账户ID
        log.setFundId(fundId);
        // 插入日志记录到数据库
        sgUserFundRecordMapper.insert(log);
    }

    /**
     * 直接归还：从系统总账户扣除，归还到用户可用账户（用于补偿）
     */
    @Override
    public void directRefund(Long userId, Integer money) {
        // 获取用户的积分账户信息
        SgUserFund userFund = getUserSgUserFund(userId);
        // 获取系统总账户信息（userId=0L代表系统总账户）
        SgUserFund allFund = getUserSgUserFund(0L);

        // 第一步：从系统总账户扣除积分（归还给用户）
        long temp = allFund.getScore() - money;
        // 判断系统总账户积分是否足够（理论上一定足够，因为是补偿操作）
        if(temp >= 0) {
            allFund.setScore(temp);
            // 记录系统总账户积分减少的日志
            saveLog(0, -money, allFund.getId());
            // 更新系统总账户记录到数据库
            int updated1 = sgUserFundMapper.updateById(allFund);
            if (updated1 == 0) {
                throw new CustomException("积分归还失败，请重试");
            }

            // 第二步：将积分增加到用户可用账户
            userFund.setScore(userFund.getScore() + money);
            // 记录用户可用积分增加的日志
            saveLog(0, money, userFund.getId());
            // 更新用户积分记录到数据库
            int updated2 = sgUserFundMapper.updateById(userFund);
            if (updated2 == 0) {
                throw new CustomException("积分归还失败，请重试");
            }
        } else {
            // 系统总账户积分不足（异常情况，需要人工介入）
            throw new CustomException("系统积分异常，请联系管理员");
        }
    }

    /**
     * 获取用户积分账户，不存在则自动创建（userId=0为系统总账户）
     */
    public SgUserFund getUserSgUserFund(long userId){
        // 根据用户ID查询积分账户
        SgUserFund sgUserFund = sgUserFundMapper.selectOne(Wrappers.<SgUserFund>lambdaQuery().eq(SgUserFund::getUserId, userId));
        // 如果账户不存在，自动创建新账户
        if(sgUserFund==null){
            sgUserFund = new SgUserFund();
            // 设置用户ID
            sgUserFund.setUserId(userId);
            // 初始化可用积分为0
            sgUserFund.setScore(0L);
            // 初始化冻结积分为0
            sgUserFund.setFreezeScore(0L);
            // 初始化版本号为0（用于乐观锁）
            sgUserFund.setVersion(0L);
            // 插入新账户记录到数据库
            sgUserFundMapper.insert(sgUserFund);
        }
        // 返回账户对象
        return sgUserFund;
    }
}
