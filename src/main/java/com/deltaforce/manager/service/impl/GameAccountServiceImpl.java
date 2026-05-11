package com.deltaforce.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.deltaforce.manager.constant.GameMonitorConstants;
import com.deltaforce.manager.entity.GameAccount;
import com.deltaforce.manager.mapper.GameAccountMapper;
import com.deltaforce.manager.service.IGameAccountService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GameAccountServiceImpl extends ServiceImpl<GameAccountMapper, GameAccount> implements IGameAccountService {

    @Override
    public GameAccount getByDeviceToken(String deviceToken) {
        return getOne(new LambdaQueryWrapper<GameAccount>()
                .eq(GameAccount::getDeviceToken, deviceToken)
                .eq(GameAccount::getStatus, GameMonitorConstants.STATUS_ENABLED)
                .last("LIMIT 1"), false);
    }

    @Override
    public String generateDeviceToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
