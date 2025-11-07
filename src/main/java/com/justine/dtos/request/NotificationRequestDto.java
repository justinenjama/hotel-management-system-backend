package com.justine.dtos.request;

import lombok.Data;

@Data
public class NotificationRequestDto {
    private String title;
    private String message;
    private String severity;
}
