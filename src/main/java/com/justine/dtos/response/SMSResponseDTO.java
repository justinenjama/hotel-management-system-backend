package com.justine.dtos.response;

import lombok.Data;

import java.util.List;

@Data
public class SMSResponseDTO {

    private List<Recipient> recipients;

    @Data
    public static class Recipient {
        private String number;
        private String status;
        private String messageId;
        private Integer cost;
    }
}

