package com.deltaforce.manager.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class MultiAccountSummaryVO {

    private BigDecimal totalBalance;
    private BigDecimal totalDailyProfit;
    private List<AccountSnapshot> accounts;

    @Data
    public static class AccountSnapshot {
        private Long accountId;
        private String accountName;
        private String deviceModel;
        private BigDecimal currentBalance;
        private BigDecimal dailyChange;
        private Date lastUpdateTime;
        private String status;
    }
}
