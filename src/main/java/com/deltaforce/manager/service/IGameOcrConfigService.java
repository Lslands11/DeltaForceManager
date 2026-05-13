package com.deltaforce.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.deltaforce.manager.entity.GameOcrConfig;

import java.util.List;

public interface IGameOcrConfigService extends IService<GameOcrConfig> {

    GameOcrConfig getByGameName(String gameName);

    List<String> listGameNames();
}
