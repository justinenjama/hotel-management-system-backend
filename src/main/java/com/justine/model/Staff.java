package com.justine.model;

import jakarta.persistence.*;
import lombok.*;
import com.justine.enums.StaffRole;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Enumerated(EnumType.STRING)
    private StaffRole role;

    private String email;
    private String phoneNumber;

    private String password;

    private String gender;

    private String accessToken;
    private String refreshToken;

    @ManyToOne
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;
}

