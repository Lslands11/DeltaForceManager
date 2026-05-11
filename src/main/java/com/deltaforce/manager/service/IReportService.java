package com.deltaforce.manager.service;

import com.deltaforce.manager.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface IReportService {

    List<DailyBalanceSummaryVO> getDailyTrend(Long accountId, LocalDate startDate, LocalDate endDate);

    MultiAccountSummaryVO getMultiAccountSummary();

    AccountBalanceTrendVO getAccountTrend(Long accountId, int days);

    ProfitReportVO getProfitSummary(String period, int offset);
}
