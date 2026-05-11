package com.deltaforce.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.deltaforce.manager.entity.GameScreenshotLog;

import java.util.List;

public interface IGameScreenshotLogService extends IService<GameScreenshotLog> {

    List<GameScreenshotLog> listPendingOcr(int limit);

    List<GameScreenshotLog> listPendingReview();
}
