package com.justine.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String reply;


    private Boolean isReplied;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_by_id")
    private Staff repliedBy;


    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
}
