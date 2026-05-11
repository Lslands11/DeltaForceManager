package com.deltaforce.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.dto.*;
import com.deltaforce.manager.entity.GameAccount;
import com.deltaforce.manager.entity.GameBalanceRecord;
import com.deltaforce.manager.service.IGameAccountService;
import com.deltaforce.manager.service.IGameBalanceRecordService;
import com.deltaforce.manager.service.IReportService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements IReportService {

    @Resource
    private IGameAccountService accountService;
    @Resource
    private IGameBalanceRecordService balanceRecordService;

    @Override
    public List<DailyBalanceSummaryVO> getDailyTrend(Long accountId, LocalDate startDate, LocalDate endDate) {
        Date start = toDate(startDate.atStartOfDay());
        Date end = toDate(endDate.atTime(LocalTime.MAX));

        LambdaQueryWrapper<GameBalanceRecord> wrapper = new LambdaQueryWrapper<GameBalanceRecord>()
                .ge(GameBalanceRecord::getRecordTime, start)
                .le(GameBalanceRecord::getRecordTime, end)
                .orderByAsc(GameBalanceRecord::getRecordTime);
        if (accountId != null) {
            wrapper.eq(GameBalanceRecord::getAccountId, accountId);
        }

        List<GameBalanceRecord> records = balanceRecordService.list(wrapper);
        Map<Long, String> accountNameMap = getAccountNameMap();

        Map<String, Map<Long, List<GameBalanceRecord>>> grouped = records.stream()
                .collect(Collectors.groupingBy(
                        r -> new SimpleDateFormat("yyyy-MM-dd").format(r.getRecordTime()),
                        LinkedHashMap::new,
                        Collectors.groupingBy(GameBalanceRecord::getAccountId)
                ));

        List<DailyBalanceSummaryVO> result = new ArrayList<>();
        for (Map.Entry<String, Map<Long, List<GameBalanceRecord>>> dayEntry : grouped.entrySet()) {
            for (Map.Entry<Long, List<GameBalanceRecord>> accountEntry : dayEntry.getValue().entrySet()) {
                List<GameBalanceRecord> dayRecords = accountEntry.getValue();
                DailyBalanceSummaryVO vo = new DailyBalanceSummaryVO();
                vo.setDate(dayEntry.getKey());
                vo.setAccountId(accountEntry.getKey());
                vo.setAccountName(accountNameMap.getOrDefault(accountEntry.getKey(), ""));
                vo.setOpenBalance(dayRecords.get(0).getBalance());
                vo.setCloseBalance(dayRecords.get(dayRecords.size() - 1).getBalance());
                vo.setDailyProfit(vo.getCloseBalance().subtract(vo.getOpenBalance()));
                vo.setRecordCount(dayRecords.size());
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public MultiAccountSummaryVO getMultiAccountSummary() {
        List<GameAccount> accounts = accountService.list(new LambdaQueryWrapper<GameAccount>()
                .eq(GameAccount::getStatus, 1));

        LocalDate today = LocalDate.now();
        Date todayStart = toDate(today.atStartOfDay());

        MultiAccountSummaryVO summary = new MultiAccountSummaryVO();
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalDailyProfit = BigDecimal.ZERO;
        List<MultiAccountSummaryVO.AccountSnapshot> snapshots = new ArrayList<>();

        for (GameAccount account : accounts) {
            GameBalanceRecord latest = balanceRecordService.getLatestByAccountId(account.getId());

            MultiAccountSummaryVO.AccountSnapshot snapshot = new MultiAccountSummaryVO.AccountSnapshot();
            snapshot.setAccountId(account.getId());
            snapshot.setAccountName(account.getAccountName());
            snapshot.setDeviceModel(account.getDeviceModel());

            if (latest != null) {
                snapshot.setCurrentBalance(latest.getBalance());
                snapshot.setLastUpdateTime(latest.getRecordTime());
                totalBalance = totalBalance.add(latest.getBalance());

                GameBalanceRecord todayFirst = balanceRecordService.getOne(new LambdaQueryWrapper<GameBalanceRecord>()
                        .eq(GameBalanceRecord::getAccountId, account.getId())
                        .ge(GameBalanceRecord::getRecordTime, todayStart)
                        .orderByAsc(GameBalanceRecord::getRecordTime)
                        .last("LIMIT 1"), false);

                if (todayFirst != null) {
                    BigDecimal dailyChange = latest.getBalance().subtract(todayFirst.getBalance());
                    snapshot.setDailyChange(dailyChange);
                    totalDailyProfit = totalDailyProfit.add(dailyChange);
                } else {
                    snapshot.setDailyChange(BigDecimal.ZERO);
                }

                long hoursSinceUpdate = (System.currentTimeMillis() - latest.getRecordTime().getTime()) / (1000 * 3600);
                snapshot.setStatus(hoursSinceUpdate <= 24 ? "online" : "offline");
            } else {
                snapshot.setCurrentBalance(BigDecimal.ZERO);
                snapshot.setDailyChange(BigDecimal.ZERO);
                snapshot.setStatus("offline");
            }

            snapshots.add(snapshot);
        }

        summary.setTotalBalance(totalBalance);
        summary.setTotalDailyProfit(totalDailyProfit);
        summary.setAccounts(snapshots);
        return summary;
    }

    @Override
    public AccountBalanceTrendVO getAccountTrend(Long accountId, int days) {
        GameAccount account = accountService.getById(accountId);
        Date startDate = toDate(LocalDate.now().minusDays(days).atStartOfDay());

        List<GameBalanceRecord> records = balanceRecordService.list(new LambdaQueryWrapper<GameBalanceRecord>()
                .eq(GameBalanceRecord::getAccountId, accountId)
                .ge(GameBalanceRecord::getRecordTime, startDate)
                .orderByAsc(GameBalanceRecord::getRecordTime));

        AccountBalanceTrendVO vo = new AccountBalanceTrendVO();
        vo.setAccountId(accountId);
        vo.setAccountName(account != null ? account.getAccountName() : "");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<AccountBalanceTrendVO.TrendPoint> points = records.stream().map(r -> {
            AccountBalanceTrendVO.TrendPoint point = new AccountBalanceTrendVO.TrendPoint();
            point.setDatetime(r.getRecordTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(dtf));
            point.setBalance(r.getBalance());
            return point;
        }).collect(Collectors.toList());

        vo.setPoints(points);
        return vo;
    }

    @Override
    public ProfitReportVO getProfitSummary(String period, int offset) {
        LocalDate now = LocalDate.now();
        LocalDate periodStart;
        LocalDate periodEnd;
        String periodLabel;

        if ("week".equals(period)) {
            LocalDate weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset);
            periodStart = weekStart;
            periodEnd = weekStart.plusDays(6);
            periodLabel = periodStart.getYear() + "-W" + periodStart.format(DateTimeFormatter.ofPattern("ww"));
        } else {
            LocalDate monthStart = now.withDayOfMonth(1).minusMonths(offset);
            periodStart = monthStart;
            periodEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
            periodLabel = periodStart.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        Date start = toDate(periodStart.atStartOfDay());
        Date end = toDate(periodEnd.atTime(LocalTime.MAX));

        List<GameAccount> accounts = accountService.list(new LambdaQueryWrapper<GameAccount>()
                .eq(GameAccount::getStatus, 1));

        ProfitReportVO report = new ProfitReportVO();
        report.setPeriod(period);
        report.setPeriodLabel(periodLabel);
        BigDecimal totalProfit = BigDecimal.ZERO;
        List<ProfitReportVO.AccountProfit> accountProfits = new ArrayList<>();

        for (GameAccount account : accounts) {
            List<GameBalanceRecord> records = balanceRecordService.list(new LambdaQueryWrapper<GameBalanceRecord>()
                    .eq(GameBalanceRecord::getAccountId, account.getId())
                    .ge(GameBalanceRecord::getRecordTime, start)
                    .le(GameBalanceRecord::getRecordTime, end)
                    .orderByAsc(GameBalanceRecord::getRecordTime));

            ProfitReportVO.AccountProfit ap = new ProfitReportVO.AccountProfit();
            ap.setAccountId(account.getId());
            ap.setAccountName(account.getAccountName());
            ap.setRecordCount(records.size());

            if (!records.isEmpty()) {
                ap.setStartBalance(records.get(0).getBalance());
                ap.setEndBalance(records.get(records.size() - 1).getBalance());
                ap.setProfit(ap.getEndBalance().subtract(ap.getStartBalance()));
                totalProfit = totalProfit.add(ap.getProfit());
            } else {
                ap.setStartBalance(BigDecimal.ZERO);
                ap.setEndBalance(BigDecimal.ZERO);
                ap.setProfit(BigDecimal.ZERO);
            }
            accountProfits.add(ap);
        }

        report.setTotalProfit(totalProfit);
        report.setAccounts(accountProfits);
        return report;
    }

    private Map<Long, String> getAccountNameMap() {
        return accountService.list().stream()
                .collect(Collectors.toMap(GameAccount::getId, GameAccount::getAccountName, (a, b) -> a));
    }

    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
