package com.deltaforce.manager.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.constant.GameMonitorConstants;
import com.deltaforce.manager.entity.GameBalanceRecord;
import com.deltaforce.manager.entity.GameOcrConfig;
import com.deltaforce.manager.entity.GameScreenshotLog;
import com.deltaforce.manager.ocr.OcrPipeline;
import com.deltaforce.manager.ocr.OcrPipeline.OcrResult;
import com.deltaforce.manager.service.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;

@Service
@Slf4j
public class OcrProcessServiceImpl implements IOcrProcessService {

    @Resource
    private IGameScreenshotLogService screenshotLogService;
    @Resource
    private IGameOcrConfigService ocrConfigService;
    @Resource
    private IGameBalanceRecordService balanceRecordService;
    @Resource
    private OcrPipeline ocrPipeline;
    @Resource
    private FileStorageService fileStorageService;

    @Async("gameMonitorExecutor")
    @Override
    public void processScreenshot(Long screenshotLogId) {
        doProcess(screenshotLogId);
    }

    @Override
    public void reprocessScreenshot(Long screenshotLogId) {
        doProcess(screenshotLogId);
    }

    private void doProcess(Long screenshotLogId) {
        GameScreenshotLog screenshotLog = screenshotLogService.getById(screenshotLogId);
        if (screenshotLog == null) {
            log.warn("截图日志不存在: {}", screenshotLogId);
            return;
        }

        GameOcrConfig ocrConfig = ocrConfigService.getByAccountId(screenshotLog.getAccountId());
        if (ocrConfig == null) {
            screenshotLog.setOcrStatus(GameMonitorConstants.OCR_STATUS_FAILED);
            screenshotLog.setErrorMessage("未配置OCR参数");
            screenshotLogService.updateById(screenshotLog);
            return;
        }

        long startTime = System.currentTimeMillis();
        try (InputStream is = fileStorageService.download(screenshotLog.getOriginalUrl())) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new RuntimeException("无法解析图片: " + screenshotLog.getOriginalUrl());
            }

            OcrResult result = ocrPipeline.execute(image, ocrConfig);

            screenshotLog.setOcrRawText(result.getRawText());
            screenshotLog.setOcrConfidence(result.getConfidence());
            screenshotLog.setParsedAmount(result.getAmount());
            screenshotLog.setOcrProcessTime((int) (System.currentTimeMillis() - startTime));

            if (result.getConfidence() != null
                    && result.getConfidence().doubleValue() >= GameMonitorConstants.CONFIDENCE_THRESHOLD
                    && result.getAmount() != null) {
                screenshotLog.setOcrStatus(GameMonitorConstants.OCR_STATUS_SUCCESS);
                createBalanceRecord(screenshotLog, result.getAmount(), GameMonitorConstants.SOURCE_OCR);
            } else {
                screenshotLog.setOcrStatus(GameMonitorConstants.OCR_STATUS_REVIEW);
            }
        } catch (Exception e) {
            log.error("OCR处理失败, screenshotLogId={}", screenshotLogId, e);
            screenshotLog.setOcrStatus(GameMonitorConstants.OCR_STATUS_FAILED);
            screenshotLog.setErrorMessage(e.getMessage());
            screenshotLog.setOcrProcessTime((int) (System.currentTimeMillis() - startTime));
        }

        screenshotLogService.updateById(screenshotLog);
    }

    private void createBalanceRecord(GameScreenshotLog screenshotLog, BigDecimal amount, int source) {
        BigDecimal change = balanceRecordService.computeBalanceChange(screenshotLog.getAccountId(), amount);

        GameBalanceRecord record = new GameBalanceRecord();
        record.setAccountId(screenshotLog.getAccountId());
        record.setScreenshotLogId(screenshotLog.getId());
        record.setBalance(amount);
        record.setBalanceChange(change);
        record.setRecordTime(screenshotLog.getUploadTime());
        record.setSource(source);
        record.setCreateTime(new Date());
        balanceRecordService.save(record);
    }
}
