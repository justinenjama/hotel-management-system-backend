package com.justine.serviceImpl;

import com.justine.dtos.TestimonialCommentDTO;
import com.justine.dtos.TestimonialDTO;
import com.justine.model.*;
import com.justine.repository.*;
import com.justine.service.AuditLogService;
import com.justine.service.TestimonialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TestimonialServiceImpl implements TestimonialService {

    private final GuestRepository guestRepository;
    private final StaffRepository staffRepository;
    private final TestimonialRepository testimonialRepository;
    private final TestimonialCommentRepository testimonialCommentRepository;
    private final TestimonialLikeRepository testimonialLikeRepository;
    private final AuditLogService auditLogService;

    public TestimonialServiceImpl(
            GuestRepository guestRepository,
            StaffRepository staffRepository,
            TestimonialRepository testimonialRepository,
            TestimonialCommentRepository testimonialCommentRepository,
            TestimonialLikeRepository testimonialLikeRepository,
            AuditLogService auditLogService
    ) {
        this.guestRepository = guestRepository;
        this.staffRepository = staffRepository;
        this.testimonialRepository = testimonialRepository;
        this.testimonialCommentRepository = testimonialCommentRepository;
        this.testimonialLikeRepository = testimonialLikeRepository;
        this.auditLogService = auditLogService;
    }

    // ✅ Helper to get authenticated Guest safely
    private Guest getGuestById(Long userId) {
        return guestRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Guest not found with ID: " + userId));
    }

    // ✅ Check if this user is admin
    private boolean isAdmin(Long userId) {
        return staffRepository.findById(userId)
                .map(staff -> "ADMIN".equalsIgnoreCase(String.valueOf(staff.getRole())))
                .orElse(false);
    }

    // ✅ Centralized Comment Mapper
    private TestimonialCommentDTO toCommentDTO(TestimonialComment comment) {
        if (comment == null) return null;

        return TestimonialCommentDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .guestId(comment.getGuest().getId())
                .guestName(comment.getGuest().getFullName())
                .build();
    }

    // ✅ Centralized Testimonial Mapper
    private TestimonialDTO toDTO(Testimonial testimonial) {
        if (testimonial == null) return null;

        return TestimonialDTO.builder()
                .id(testimonial.getId())
                .content(testimonial.getContent())
                .createdAt(testimonial.getCreatedAt())
                .updatedAt(testimonial.getUpdatedAt())
                .guestId(testimonial.getGuest().getId())
                .guestName(testimonial.getGuest().getFullName())
                .likeCount(Optional.ofNullable(testimonial.getLikes()).map(List::size).orElse(0))
                .comments(Optional.ofNullable(testimonial.getComments())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(this::toCommentDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public TestimonialDTO addTestimonial(String content, Long currentUserId) {
        try {
            Guest guest = getGuestById(currentUserId);

            Testimonial testimonial = Testimonial.builder()
                    .content(content)
                    .guest(guest)
                    .build();

            testimonialRepository.save(testimonial);

            auditLogService.logTestimonial(currentUserId, "ADD_TESTIMONIAL", testimonial.getId(),
                    Map.of("content", content));

            return toDTO(testimonial);
        } catch (Exception e) {
            log.error("❌ Failed to add testimonial: {}", e.getMessage());
            throw e;
        }
    }

    // ✅ Create Comment + Audit Log
    @Override
    public TestimonialCommentDTO addComment(Long testimonialId, String content, Long currentUserId) {
        try {
            Guest guest = getGuestById(currentUserId);
            Testimonial testimonial = testimonialRepository.findById(testimonialId)
                    .orElseThrow(() -> new RuntimeException("Testimonial not found"));

            TestimonialComment comment = TestimonialComment.builder()
                    .testimonial(testimonial)
                    .guest(guest)
                    .content(content)
                    .build();

            testimonialCommentRepository.save(comment);

            auditLogService.logComment(currentUserId, "ADD_COMMENT", comment.getId(),
                    Map.of("testimonialId", testimonialId, "content", content));

            return toCommentDTO(comment);
        } catch (Exception e) {
            log.error("❌ Failed to add comment: {}", e.getMessage());
            throw e;
        }
    }

    // ✅ Toggle Like + Audit Log
    @Override
    public String toggleLike(Long testimonialId, Long currentUserId) {
        try {
            Guest guest = getGuestById(currentUserId);
            Testimonial testimonial = testimonialRepository.findById(testimonialId)
                    .orElseThrow(() -> new RuntimeException("Testimonial not found"));

            Optional<TestimonialLike> existing = testimonialLikeRepository.findByTestimonialAndGuest(testimonial, guest);

            if (existing.isPresent()) {
                testimonialLikeRepository.delete(existing.get());

                auditLogService.logLike(currentUserId, "REMOVE_LIKE", existing.get().getId(),
                        Map.of("testimonialId", testimonialId));

                return "Like removed";
            } else {
                TestimonialLike like = testimonialLikeRepository.save(TestimonialLike.builder()
                        .testimonial(testimonial)
                        .guest(guest)
                        .build());

                auditLogService.logLike(currentUserId, "ADD_LIKE", like.getId(),
                        Map.of("testimonialId", testimonialId));

                return "Like added";
            }
        } catch (Exception e) {
            log.error("❌ Failed to toggle like: {}", e.getMessage());
            throw e;
        }
    }

    // ✅ Get All
    @Override
    public List<TestimonialDTO> getAllTestimonials() {
        return testimonialRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ Update Testimonial + Audit Log
    @Override
    public TestimonialDTO updateTestimonial(Long testimonialId, String newContent, Long currentUserId) {
        try {
            Testimonial testimonial = testimonialRepository.findById(testimonialId)
                    .orElseThrow(() -> new RuntimeException("Testimonial not found"));

            if (!testimonial.getGuest().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
                throw new RuntimeException("Unauthorized to update this testimonial");
            }

            testimonial.setContent(newContent);
            testimonialRepository.save(testimonial);

            auditLogService.logTestimonial(currentUserId, "UPDATE_TESTIMONIAL", testimonialId,
                    Map.of("newContent", newContent));

            return toDTO(testimonial);
        } catch (Exception e) {
            log.error("❌ Failed to update testimonial: {}", e.getMessage());
            throw e;
        }
    }

    // ✅ Delete Testimonial + Audit Log
    @Override
    public boolean deleteTestimonial(Long testimonialId, Long currentUserId) {
        try {
            Testimonial testimonial = testimonialRepository.findById(testimonialId)
                    .orElseThrow(() -> new RuntimeException("Testimonial not found"));

            if (!testimonial.getGuest().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
                throw new RuntimeException("Unauthorized to delete testimonial");
            }

            testimonialRepository.delete(testimonial);

            auditLogService.logTestimonial(currentUserId, "DELETE_TESTIMONIAL", testimonialId,
                    Map.of("deleted", true));

            return true;
        } catch (Exception e) {
            log.error("❌ Failed to delete testimonial: {}", e.getMessage());
            throw e;
        }
    }

    // ✅ Get Comments
    @Override
    public List<TestimonialCommentDTO> getCommentsByTestimonialId(Long testimonialId) {
        Testimonial testimonial = testimonialRepository.findById(testimonialId)
                .orElseThrow(() -> new RuntimeException("Testimonial not found"));

        return testimonialCommentRepository.findByTestimonial(testimonial)
                .stream()
                .map(this::toCommentDTO)
                .collect(Collectors.toList());
    }

    // ✅ Update Comment + Audit Log
    @Override
    public TestimonialCommentDTO updateComment(Long testimonialId, Long commentId, String newContent, Long currentUserId) {
        try {
            TestimonialComment comment = testimonialCommentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));

            if (!comment.getTestimonial().getId().equals(testimonialId)) {
                throw new RuntimeException("Comment does not belong to this testimonial");
            }

            if (!comment.getGuest().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
                throw new RuntimeException("Unauthorized to update comment");
            }

            comment.setContent(newContent);
            testimonialCommentRepository.save(comment);

            auditLogService.logComment(currentUserId, "UPDATE_COMMENT", commentId,
                    Map.of("newContent", newContent));

            return toCommentDTO(comment);
        } catch (Exception e) {
            log.error("❌ Failed to update comment: {}", e.getMessage());
            throw e;
        }
    }

    // ✅ Delete Comment + Audit Log
    @Override
    public boolean deleteComment(Long testimonialId, Long commentId, Long currentUserId) {
        try {
            TestimonialComment comment = testimonialCommentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));

            if (!comment.getTestimonial().getId().equals(testimonialId)) {
                throw new RuntimeException("Comment does not belong to this testimonial");
            }

            if (!comment.getGuest().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
                throw new RuntimeException("Unauthorized to delete comment");
            }

            testimonialCommentRepository.delete(comment);

            auditLogService.logComment(currentUserId, "DELETE_COMMENT", commentId,
                    Map.of("deleted", true));

            return true;
        } catch (Exception e) {
            log.error("❌ Failed to delete comment: {}", e.getMessage());
            throw e;
        }
    }
}
