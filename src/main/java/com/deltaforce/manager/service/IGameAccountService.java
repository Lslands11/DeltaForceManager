package com.deltaforce.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.deltaforce.manager.entity.GameAccount;

public interface IGameAccountService extends IService<GameAccount> {

    GameAccount getByDeviceToken(String deviceToken);

    String generateDeviceToken();
}
