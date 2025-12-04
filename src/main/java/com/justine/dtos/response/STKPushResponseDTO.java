package com.justine.dtos.response;

import lombok.Data;

@Data
public class STKPushResponseDTO {
    private String merchantRequestId;
    private String checkoutRequestId;
    private String responseCode;
    private String responseDescription;
    private String customerMessage;
}

