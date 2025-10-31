package com.justine.dtos;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestimonialDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long guestId;
    private String guestName;
    private int likeCount;
    private List<TestimonialCommentDTO> comments;
}
