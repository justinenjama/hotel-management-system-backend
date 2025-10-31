package com.justine.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"testimonial_id", "guest_id"})
})
public class TestimonialLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "testimonial_id", nullable = false)
    private Testimonial testimonial;

    @ManyToOne
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;
}
