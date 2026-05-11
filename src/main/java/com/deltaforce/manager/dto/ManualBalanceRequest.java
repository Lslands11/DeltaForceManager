package com.deltaforce.manager.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ManualBalanceRequest {

    private Long accountId;
    private BigDecimal balance;
    private String remark;
}
