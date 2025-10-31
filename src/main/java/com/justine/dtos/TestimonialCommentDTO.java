package com.justine.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestimonialCommentDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long guestId;
    private String guestName;
}
