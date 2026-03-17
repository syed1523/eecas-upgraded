package com.expense.system.repository;

import com.expense.system.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityIdAndEntityTypeOrderByTimestampDesc(Long entityId, String entityType);

    List<AuditLog> findByPerformedByOrderByTimestampDesc(String performedBy);

    List<AuditLog> findTop50ByOrderByTimestampDesc();

    List<AuditLog> findByTimestampAfterOrderByTimestampDesc(LocalDateTime after);

    List<AuditLog> findByWasSystemTriggeredTrueOrderByTimestampDesc();

    // --- Legacy methods for compatibility ---
    List<AuditLog> findByEntityIdAndEntityType(Long entityId, String entityType);

    java.util.Optional<AuditLog> findFirstByOrderByTimestampDesc();

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
