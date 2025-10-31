package com.justine.dtos.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactRequestDto {
    private String fullName;
    private String email;
    private String message;
}
