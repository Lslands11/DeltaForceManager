package com.deltaforce.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.deltaforce.manager.entity.GameOcrConfig;
import com.deltaforce.manager.mapper.GameOcrConfigMapper;
import com.deltaforce.manager.service.IGameOcrConfigService;
import org.springframework.stereotype.Service;

@Service
public class GameOcrConfigServiceImpl extends ServiceImpl<GameOcrConfigMapper, GameOcrConfig> implements IGameOcrConfigService {

    @Override
    public GameOcrConfig getByAccountId(Long accountId) {
        return getOne(new LambdaQueryWrapper<GameOcrConfig>()
                .eq(GameOcrConfig::getAccountId, accountId)
                .last("LIMIT 1"), false);
    }
}
