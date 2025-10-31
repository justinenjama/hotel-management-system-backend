package com.justine.dtos.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyRequestDto {
    private Long staffId;  // optional for logging who replied
    private String reply;  // admin’s reply message
}
