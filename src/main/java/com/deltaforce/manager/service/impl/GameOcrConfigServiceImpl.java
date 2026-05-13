package com.deltaforce.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.deltaforce.manager.entity.GameOcrConfig;
import com.deltaforce.manager.mapper.GameOcrConfigMapper;
import com.deltaforce.manager.service.IGameOcrConfigService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameOcrConfigServiceImpl extends ServiceImpl<GameOcrConfigMapper, GameOcrConfig> implements IGameOcrConfigService {

    @Override
    public GameOcrConfig getByGameName(String gameName) {
        return getOne(new LambdaQueryWrapper<GameOcrConfig>()
                .eq(GameOcrConfig::getGameName, gameName)
                .last("LIMIT 1"), false);
    }

    @Override
    public List<String> listGameNames() {
        return listObjs(new LambdaQueryWrapper<GameOcrConfig>()
                .select(GameOcrConfig::getGameName), Object::toString)
                .stream()
                .collect(Collectors.toList());
    }
}
