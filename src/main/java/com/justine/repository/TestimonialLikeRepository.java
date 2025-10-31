package com.justine.repository;

import com.justine.model.TestimonialLike;
import com.justine.model.Testimonial;
import com.justine.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TestimonialLikeRepository extends JpaRepository<TestimonialLike, Long> {
    Optional<TestimonialLike> findByTestimonialAndGuest(Testimonial testimonial, Guest guest);
    long countByTestimonial(Testimonial testimonial);
}
