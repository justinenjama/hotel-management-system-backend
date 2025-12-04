package com.justine.wifi.dtos.request;

import lombok.Data;

@Data
public class SMSRequestDTO {
    private String phone;
    private String message;
}

