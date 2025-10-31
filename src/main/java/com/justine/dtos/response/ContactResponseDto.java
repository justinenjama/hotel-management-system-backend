package com.justine.dtos.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactResponseDto {
    private Long id;
    private String fullName;
    private String email;
    private String message;
    private String reply;
    private Boolean isReplied;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
}
