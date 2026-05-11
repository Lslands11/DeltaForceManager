package com.deltaforce.manager.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProfitReportVO {

    private String period;
    private String periodLabel;
    private BigDecimal totalProfit;
    private List<AccountProfit> accounts;

    @Data
    public static class AccountProfit {
        private Long accountId;
        private String accountName;
        private BigDecimal startBalance;
        private BigDecimal endBalance;
        private BigDecimal profit;
        private Integer recordCount;
    }
}
