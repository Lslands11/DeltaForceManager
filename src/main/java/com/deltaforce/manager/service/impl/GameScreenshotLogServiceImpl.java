package com.deltaforce.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.deltaforce.manager.constant.GameMonitorConstants;
import com.deltaforce.manager.entity.GameScreenshotLog;
import com.deltaforce.manager.mapper.GameScreenshotLogMapper;
import com.deltaforce.manager.service.IGameScreenshotLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameScreenshotLogServiceImpl extends ServiceImpl<GameScreenshotLogMapper, GameScreenshotLog> implements IGameScreenshotLogService {

    @Override
    public List<GameScreenshotLog> listPendingOcr(int limit) {
        return list(new LambdaQueryWrapper<GameScreenshotLog>()
                .eq(GameScreenshotLog::getOcrStatus, GameMonitorConstants.OCR_STATUS_PENDING)
                .orderByAsc(GameScreenshotLog::getCreateTime)
                .last("LIMIT " + limit));
    }

    @Override
    public List<GameScreenshotLog> listPendingReview() {
        return list(new LambdaQueryWrapper<GameScreenshotLog>()
                .eq(GameScreenshotLog::getOcrStatus, GameMonitorConstants.OCR_STATUS_REVIEW)
                .orderByAsc(GameScreenshotLog::getCreateTime));
    }
}
