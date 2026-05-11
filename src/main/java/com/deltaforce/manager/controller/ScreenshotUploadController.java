package com.deltaforce.manager.controller;

import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.dto.Result;
import com.deltaforce.manager.dto.ScreenshotUploadResponse;
import com.deltaforce.manager.entity.GameAccount;
import com.deltaforce.manager.entity.GameScreenshotLog;
import com.deltaforce.manager.service.IGameAccountService;
import com.deltaforce.manager.service.IGameScreenshotLogService;
import com.deltaforce.manager.service.IOcrProcessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/device")
@Slf4j
public class ScreenshotUploadController {

    @Resource
    private IGameAccountService accountService;
    @Resource
    private IGameScreenshotLogService screenshotLogService;
    @Resource
    private IOcrProcessService ocrProcessService;

    @Value("${game-monitor.upload.path:uploads/screenshots}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<ScreenshotUploadResponse> uploadScreenshot(
            @RequestParam("device_token") String deviceToken,
            @RequestParam("screenshot") MultipartFile screenshot) {

        GameAccount account = accountService.getByDeviceToken(deviceToken);
        if (account == null) {
            return Result.error("无效的device_token");
        }

        if (screenshot.isEmpty()) {
            return Result.error("截图文件为空");
        }

        try {
            String fileName = UUID.randomUUID().toString().replace("-", "")
                    + getExtension(screenshot.getOriginalFilename());

            File dir = new File(uploadPath, String.valueOf(account.getId()));
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File destFile = new File(dir, fileName);
            screenshot.transferTo(destFile);

            GameScreenshotLog screenshotLog = new GameScreenshotLog();
            screenshotLog.setAccountId(account.getId());
            screenshotLog.setOriginalUrl(destFile.getAbsolutePath());
            screenshotLog.setUploadTime(new Date());
            screenshotLog.setOcrStatus(0);
            screenshotLog.setCreateTime(new Date());
            screenshotLogService.save(screenshotLog);

            ocrProcessService.processScreenshot(screenshotLog.getId());

            ScreenshotUploadResponse response = new ScreenshotUploadResponse();
            response.setScreenshotId(String.valueOf(screenshotLog.getId()));
            response.setMessage("上传成功");
            return Result.OK(response);

        } catch (IOException e) {
            log.error("截图上传失败", e);
            return Result.error("截图上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/heartbeat")
    public Result<String> heartbeat(@RequestParam("device_token") String deviceToken) {
        GameAccount account = accountService.getByDeviceToken(deviceToken);
        if (account == null) {
            return Result.error("无效的device_token");
        }
        return Result.OK("ok");
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".png";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
