package com.deltaforce.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.constant.GameMonitorConstants;
import com.deltaforce.manager.dto.Result;
import com.deltaforce.manager.dto.ScreenshotUploadResponse;
import com.deltaforce.manager.entity.GameBalanceRecord;
import com.deltaforce.manager.entity.GameScreenshotLog;
import com.deltaforce.manager.service.FileStorageService;
import com.deltaforce.manager.service.IGameAccountService;
import com.deltaforce.manager.service.IGameBalanceRecordService;
import com.deltaforce.manager.service.IGameScreenshotLogService;
import com.deltaforce.manager.service.IOcrProcessService;
import com.deltaforce.manager.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/screenshot")
@Slf4j
public class ScreenshotLogController {

    @Resource
    private IGameScreenshotLogService screenshotLogService;
    @Resource
    private IOcrProcessService ocrProcessService;
    @Resource
    private IGameBalanceRecordService balanceRecordService;
    @Resource
    private IGameAccountService gameAccountService;
    @Resource
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Result<ScreenshotUploadResponse> uploadScreenshot(
            @RequestParam("accountId") Long accountId,
            @RequestParam("screenshot") MultipartFile screenshot) {

        if (screenshot.isEmpty()) {
            return Result.error("截图文件为空");
        }

        // 非管理员只能上传自己名下账号的截图
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            List<Long> accountIds = gameAccountService.getAccountIdsByUserId(userId);
            if (!accountIds.contains(accountId)) {
                return Result.error("无权上传该账号的截图");
            }
        }

        try {
            String objectPath = fileStorageService.upload(
                    accountId,
                    screenshot.getOriginalFilename(),
                    screenshot.getInputStream(),
                    screenshot.getSize(),
                    screenshot.getContentType());

            GameScreenshotLog screenshotLog = new GameScreenshotLog();
            screenshotLog.setAccountId(accountId);
            screenshotLog.setOriginalUrl(objectPath);
            screenshotLog.setUploadTime(new Date());
            screenshotLog.setOcrStatus(0);
            screenshotLog.setCreateTime(new Date());
            screenshotLogService.save(screenshotLog);

            ocrProcessService.processScreenshot(screenshotLog.getId());

            ScreenshotUploadResponse response = new ScreenshotUploadResponse();
            response.setScreenshotId(String.valueOf(screenshotLog.getId()));
            response.setMessage("上传成功，OCR处理中");
            return Result.OK(response);

        } catch (IOException e) {
            log.error("截图上传失败", e);
            return Result.error("截图上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<IPage<GameScreenshotLog>> queryPageList(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "accountId", required = false) Long accountId,
            @RequestParam(name = "ocrStatus", required = false) Integer ocrStatus) {

        LambdaQueryWrapper<GameScreenshotLog> wrapper = new LambdaQueryWrapper<>();
        if (accountId != null) {
            wrapper.eq(GameScreenshotLog::getAccountId, accountId);
        }
        if (ocrStatus != null) {
            wrapper.eq(GameScreenshotLog::getOcrStatus, ocrStatus);
        }
        // 非管理员只能看到自己名下账号的截图
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            List<Long> accountIds = gameAccountService.getAccountIdsByUserId(userId);
            if (accountIds.isEmpty()) {
                return Result.OK(new Page<>(pageNo, pageSize));
            }
            wrapper.in(GameScreenshotLog::getAccountId, accountIds);
        }
        wrapper.orderByDesc(GameScreenshotLog::getUploadTime);

        Page<GameScreenshotLog> page = new Page<>(pageNo, pageSize);
        IPage<GameScreenshotLog> pageList = screenshotLogService.page(page, wrapper);
        return Result.OK(pageList);
    }

    @GetMapping("/pendingReview")
    public Result<List<GameScreenshotLog>> pendingReview() {
        List<GameScreenshotLog> list = screenshotLogService.listPendingReview();
        // 非管理员只看自己名下账号的待审核
        if (!SecurityUtil.isAdmin()) {
            Long userId = SecurityUtil.getCurrentUserId();
            List<Long> accountIds = gameAccountService.getAccountIdsByUserId(userId);
            list = list.stream()
                    .filter(s -> accountIds.contains(s.getAccountId()))
                    .toList();
        }
        return Result.OK(list);
    }

    @PutMapping("/review/{id}")
    public Result<String> review(@PathVariable("id") Long id,
                                 @RequestParam("amount") BigDecimal amount) {
        GameScreenshotLog screenshotLog = screenshotLogService.getById(id);
        if (screenshotLog == null) {
            return Result.error("截图日志不存在");
        }

        screenshotLog.setManualAmount(amount);
        screenshotLog.setOcrStatus(GameMonitorConstants.OCR_STATUS_SUCCESS);
        screenshotLogService.updateById(screenshotLog);

        BigDecimal change = balanceRecordService.computeBalanceChange(screenshotLog.getAccountId(), amount);
        GameBalanceRecord record = new GameBalanceRecord();
        record.setAccountId(screenshotLog.getAccountId());
        record.setScreenshotLogId(screenshotLog.getId());
        record.setBalance(amount);
        record.setBalanceChange(change);
        record.setRecordTime(screenshotLog.getUploadTime());
        record.setSource(GameMonitorConstants.SOURCE_CORRECTED);
        record.setCreateTime(new Date());
        balanceRecordService.save(record);

        return Result.OK("审核完成!");
    }

    @PostMapping("/reprocess/{id}")
    public Result<String> reprocess(@PathVariable("id") Long id) {
        GameScreenshotLog screenshotLog = screenshotLogService.getById(id);
        if (screenshotLog == null) {
            return Result.error("截图日志不存在");
        }
        ocrProcessService.reprocessScreenshot(id);
        return Result.OK("已重新提交OCR处理");
    }
}
