package com.deltaforce.manager.controller;

import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.dto.*;
import com.deltaforce.manager.service.IGameAccountService;
import com.deltaforce.manager.service.IReportService;
import com.deltaforce.manager.util.SecurityUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/report")
@Slf4j
public class ReportController {

    @Resource
    private IReportService reportService;
    @Resource
    private IGameAccountService gameAccountService;

    @GetMapping("/dailyTrend")
    public Result<List<DailyBalanceSummaryVO>> dailyTrend(
            @RequestParam(name = "accountId", required = false) Long accountId,
            @RequestParam(name = "startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        if (!SecurityUtil.isAdmin()) {
            if (accountId != null) {
                verifyAccountOwnership(accountId);
            } else {
                // 非管理员不指定账号时，只查自己名下的账号
                List<Long> accountIds = gameAccountService.getAccountIdsByUserId(SecurityUtil.getCurrentUserId());
                if (accountIds.isEmpty()) {
                    return Result.OK(new java.util.ArrayList<>());
                }
                // 查询所有归属账号的趋势
                List<DailyBalanceSummaryVO> all = new java.util.ArrayList<>();
                for (Long aid : accountIds) {
                    all.addAll(reportService.getDailyTrend(aid, startDate, endDate));
                }
                return Result.OK(all);
            }
        }
        return Result.OK(reportService.getDailyTrend(accountId, startDate, endDate));
    }

    @GetMapping("/multiAccountSummary")
    public Result<MultiAccountSummaryVO> multiAccountSummary() {
        if (SecurityUtil.isAdmin()) {
            return Result.OK(reportService.getMultiAccountSummary());
        }
        return Result.OK(reportService.getMultiAccountSummaryByUserId(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/accountTrend")
    public Result<AccountBalanceTrendVO> accountTrend(
            @RequestParam(name = "accountId") Long accountId,
            @RequestParam(name = "days", defaultValue = "30") int days) {
        if (!SecurityUtil.isAdmin()) {
            verifyAccountOwnership(accountId);
        }
        return Result.OK(reportService.getAccountTrend(accountId, days));
    }

    @GetMapping("/profitSummary")
    public Result<ProfitReportVO> profitSummary(
            @RequestParam(name = "period", defaultValue = "week") String period,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        if (SecurityUtil.isAdmin()) {
            return Result.OK(reportService.getProfitSummary(period, offset));
        }
        return Result.OK(reportService.getProfitSummaryByUserId(period, offset, SecurityUtil.getCurrentUserId()));
    }

    private void verifyAccountOwnership(Long accountId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Long> accountIds = gameAccountService.getAccountIdsByUserId(userId);
        if (!accountIds.contains(accountId)) {
            throw new RuntimeException("无权访问该账号数据");
        }
    }
}
