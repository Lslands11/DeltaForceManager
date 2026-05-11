package com.deltaforce.manager.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailyBalanceSummaryVO {

    private String date;
    private Long accountId;
    private String accountName;
    private BigDecimal openBalance;
    private BigDecimal closeBalance;
    private BigDecimal dailyProfit;
    private Integer recordCount;
}
