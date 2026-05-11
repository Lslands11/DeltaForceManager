package com.deltaforce.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.deltaforce.manager.entity.GameOcrConfig;

public interface IGameOcrConfigService extends IService<GameOcrConfig> {

    GameOcrConfig getByAccountId(Long accountId);
}
