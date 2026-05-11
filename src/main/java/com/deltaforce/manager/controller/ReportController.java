package com.deltaforce.manager.controller;

import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.dto.*;
import com.deltaforce.manager.service.IReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/report")
@Slf4j
public class ReportController {

    @Resource
    private IReportService reportService;

    @GetMapping("/dailyTrend")
    public Result<List<DailyBalanceSummaryVO>> dailyTrend(
            @RequestParam(name = "accountId", required = false) Long accountId,
            @RequestParam(name = "startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.OK(reportService.getDailyTrend(accountId, startDate, endDate));
    }

    @GetMapping("/multiAccountSummary")
    public Result<MultiAccountSummaryVO> multiAccountSummary() {
        return Result.OK(reportService.getMultiAccountSummary());
    }

    @GetMapping("/accountTrend")
    public Result<AccountBalanceTrendVO> accountTrend(
            @RequestParam(name = "accountId") Long accountId,
            @RequestParam(name = "days", defaultValue = "30") int days) {
        return Result.OK(reportService.getAccountTrend(accountId, days));
    }

    @GetMapping("/profitSummary")
    public Result<ProfitReportVO> profitSummary(
            @RequestParam(name = "period", defaultValue = "week") String period,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {
        return Result.OK(reportService.getProfitSummary(period, offset));
    }
}
