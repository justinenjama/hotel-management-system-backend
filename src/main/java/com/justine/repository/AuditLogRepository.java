package com.justine.repository;

import com.justine.model.AuditLog;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Retrieve recent logs for a given actor
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.actorId = :actorId
        ORDER BY a.createdAt DESC
        """)
    List<AuditLog> findRecentLogsByActor(@Param("actorId") Long actorId);

    // Retrieve logs for a specific entity type
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.entity = :entity
        ORDER BY a.createdAt DESC
        """)
    List<AuditLog> findLogsByEntity(@Param("entity") String entity);

    // Cleanup old logs
    @Transactional
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :threshold")
    int deleteOldLogs(@Param("threshold") LocalDateTime threshold);
}
