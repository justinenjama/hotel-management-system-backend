package com.justine.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class AuditLog {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long actorId;
    private String action;
    private String entity;
    private Long entityId;
    @Column(columnDefinition = "json") 
    private String metadataJson;
    private LocalDateTime createdAt;
}