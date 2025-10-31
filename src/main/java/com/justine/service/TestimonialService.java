package com.justine.service;

import com.justine.dtos.TestimonialCommentDTO;
import com.justine.dtos.TestimonialDTO;

import java.util.List;

public interface TestimonialService {

    TestimonialDTO addTestimonial(String content, Long currentUserId);

    List<TestimonialDTO> getAllTestimonials();

    TestimonialCommentDTO addComment(Long testimonialId, String content, Long currentUserId);

    String toggleLike(Long testimonialId, Long currentUserId);

    TestimonialDTO updateTestimonial(Long testimonialId, String newContent, Long currentUserId);

    boolean deleteTestimonial(Long testimonialId, Long currentUserId);

    List<TestimonialCommentDTO> getCommentsByTestimonialId(Long testimonialId);

    TestimonialCommentDTO updateComment(Long testimonialId, Long commentId, String newContent, Long currentUserId);

    boolean deleteComment(Long testimonialId, Long commentId, Long currentUserId);
}
