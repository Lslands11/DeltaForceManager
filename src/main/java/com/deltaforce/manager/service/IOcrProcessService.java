package com.deltaforce.manager.service;

public interface IOcrProcessService {

    void processScreenshot(Long screenshotLogId);

    void reprocessScreenshot(Long screenshotLogId);
}
