package com.deltaforce.manager.job;

import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.entity.GameScreenshotLog;
import com.deltaforce.manager.service.IGameScreenshotLogService;
import com.deltaforce.manager.service.IOcrProcessService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class OcrRetryJob {

    @Resource
    private IGameScreenshotLogService screenshotLogService;
    @Resource
    private IOcrProcessService ocrProcessService;

    @Scheduled(fixedDelay = 300000)
    public void retryPendingOcr() {
        List<GameScreenshotLog> pendingList = screenshotLogService.listPendingOcr(20);
        if (pendingList.isEmpty()) {
            return;
        }
        log.info("OCR重试任务: 待处理{}条", pendingList.size());
        for (GameScreenshotLog screenshotLog : pendingList) {
            try {
                ocrProcessService.reprocessScreenshot(screenshotLog.getId());
            } catch (Exception e) {
                log.error("OCR重试失败, screenshotLogId={}", screenshotLog.getId(), e);
            }
        }
    }
}
