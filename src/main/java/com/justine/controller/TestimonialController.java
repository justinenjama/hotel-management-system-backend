package com.justine.controller;

import com.justine.dtos.TestimonialDTO;
import com.justine.dtos.TestimonialCommentDTO;
import com.justine.service.TestimonialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/testimonials")
public class TestimonialController {

    private final TestimonialService testimonialService;

    public TestimonialController(TestimonialService testimonialService) {
        this.testimonialService = testimonialService;
    }

    private Long getUserId(Principal principal) {
        return Long.valueOf(principal.getName());
    }

    @PostMapping
    public ResponseEntity<TestimonialDTO> addTestimonial(
            @RequestBody Map<String, String> request,
            Principal principal
    ) {
        String content = request.get("content");
        return ResponseEntity.ok(testimonialService.addTestimonial(content, getUserId(principal)));
    }

    @GetMapping
    public ResponseEntity<List<TestimonialDTO>> getAllTestimonials() {
        List<TestimonialDTO> testimonials = testimonialService.getAllTestimonials();
        if (testimonials.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(testimonials);
    }

    @PostMapping("/{testimonialId}/comments")
    public ResponseEntity<TestimonialCommentDTO> addComment(
            @PathVariable Long testimonialId,
            @RequestBody Map<String, String> request,
            Principal principal
    ) {
        String content = request.get("content");
        return ResponseEntity.ok(testimonialService.addComment(testimonialId, content, getUserId(principal)));
    }

    @GetMapping("/{testimonialId}/comments")
    public ResponseEntity<List<TestimonialCommentDTO>> getComments(
            @PathVariable Long testimonialId
    ) {
        List<TestimonialCommentDTO> comments = testimonialService.getCommentsByTestimonialId(testimonialId);
        if (comments.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{testimonialId}/likes")
    public ResponseEntity<String> toggleLike(
            @PathVariable Long testimonialId,
            Principal principal
    ) {
        return ResponseEntity.ok(testimonialService.toggleLike(testimonialId, getUserId(principal)));
    }

    @PutMapping("/{testimonialId}")
    public ResponseEntity<TestimonialDTO> updateTestimonial(
            @PathVariable Long testimonialId,
            @RequestBody Map<String, String> request,
            Principal principal
    ) {
        String content = request.get("content");
        return ResponseEntity.ok(testimonialService.updateTestimonial(testimonialId, content, getUserId(principal)));
    }

    @DeleteMapping("/{testimonialId}")
    public ResponseEntity<Void> deleteTestimonial(
            @PathVariable Long testimonialId,
            Principal principal
    ) {
        boolean deleted = testimonialService.deleteTestimonial(testimonialId, getUserId(principal));
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/{testimonialId}/comments/{commentId}")
    public ResponseEntity<TestimonialCommentDTO> updateComment(
            @PathVariable Long testimonialId,
            @PathVariable Long commentId,
            @RequestBody Map<String, String> request,
            Principal principal
    ) {
        String content = request.get("content");
        return ResponseEntity.ok(
                testimonialService.updateComment(testimonialId, commentId, content, getUserId(principal))
        );
    }

    @DeleteMapping("/{testimonialId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long testimonialId,
            @PathVariable Long commentId,
            Principal principal
    ) {
        boolean deleted = testimonialService.deleteComment(testimonialId, commentId, getUserId(principal));
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
