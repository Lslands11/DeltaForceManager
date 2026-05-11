package com.deltaforce.manager.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountBalanceTrendVO {

    private Long accountId;
    private String accountName;
    private List<TrendPoint> points;

    @Data
    public static class TrendPoint {
        private String datetime;
        private BigDecimal balance;
    }
}
