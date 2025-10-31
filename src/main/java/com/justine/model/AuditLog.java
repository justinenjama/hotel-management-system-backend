package com.justine.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actorId"),
        @Index(name = "idx_audit_entity", columnList = "entity"),
        @Index(name = "idx_audit_created_at", columnList = "createdAt")
})
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
