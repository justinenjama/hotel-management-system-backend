package com.justine.repository;

import java.util.List;
import java.util.Optional;

import com.justine.enums.StaffRole;
import io.micrometer.common.KeyValues;
import org.springframework.data.jpa.repository.JpaRepository;

import com.justine.model.Staff;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByEmail(String email);
    boolean existsByEmail(String email);

    List<Staff> findByRole(StaffRole staffRole);
}
