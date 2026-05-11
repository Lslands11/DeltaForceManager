package com.deltaforce.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.deltaforce.manager.entity.GameBalanceRecord;

import java.math.BigDecimal;

public interface IGameBalanceRecordService extends IService<GameBalanceRecord> {

    GameBalanceRecord getLatestByAccountId(Long accountId);

    BigDecimal computeBalanceChange(Long accountId, BigDecimal newBalance);
}
