package com.deltaforce.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.deltaforce.manager.entity.GameBalanceRecord;
import com.deltaforce.manager.mapper.GameBalanceRecordMapper;
import com.deltaforce.manager.service.IGameBalanceRecordService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GameBalanceRecordServiceImpl extends ServiceImpl<GameBalanceRecordMapper, GameBalanceRecord> implements IGameBalanceRecordService {

    @Override
    public GameBalanceRecord getLatestByAccountId(Long accountId) {
        return getOne(new LambdaQueryWrapper<GameBalanceRecord>()
                .eq(GameBalanceRecord::getAccountId, accountId)
                .orderByDesc(GameBalanceRecord::getRecordTime)
                .last("LIMIT 1"), false);
    }

    @Override
    public BigDecimal computeBalanceChange(Long accountId, BigDecimal newBalance) {
        GameBalanceRecord latest = getLatestByAccountId(accountId);
        if (latest == null) {
            return BigDecimal.ZERO;
        }
        return newBalance.subtract(latest.getBalance());
    }
}
