package com.justine.dtos.response;

import lombok.Data;

@Data
public class BulkSMSResponseDTO {
    private int success;
    private int failed;
}

