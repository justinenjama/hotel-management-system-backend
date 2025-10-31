package com.justine.repository;

import com.justine.model.Testimonial;
import com.justine.model.TestimonialComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestimonialCommentRepository extends JpaRepository<TestimonialComment, Long> {
    List<TestimonialComment> findByTestimonial(Testimonial testimonial);
}
