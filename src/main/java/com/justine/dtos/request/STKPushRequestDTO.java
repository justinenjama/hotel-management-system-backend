package com.justine.dtos.request;

import lombok.Data;

@Data
public class STKPushRequestDTO {
    private String phone;
    private double amount;
    private String accountReference;
    private String transactionDesc;
}

