package com.deltaforce.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.deltaforce.manager.entity.GameAccount;

import java.util.List;

public interface IGameAccountService extends IService<GameAccount> {

    GameAccount getByDeviceToken(String deviceToken);

    String generateDeviceToken();

    String generateDeviceId(String deviceName);

    List<Long> getAccountIdsByUserId(Long userId);
}
