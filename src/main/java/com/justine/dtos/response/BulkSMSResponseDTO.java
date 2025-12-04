package com.justine.wifi.dtos.response;

import lombok.Data;

@Data
public class BulkSMSResponseDTO {
    private int success;
    private int failed;
}

