package com.justine.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestimonialComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 800)
    private String content;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest; // commenter

    @ManyToOne
    @JoinColumn(name = "testimonial_id", nullable = false)
    @JsonBackReference
    private Testimonial testimonial;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
