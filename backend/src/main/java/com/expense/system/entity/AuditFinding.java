package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_findings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long expenseId;

    @Column(nullable = false)
    private Long auditorId;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FindingStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = FindingStatus.OPEN;
        }
    }

    public enum FindingStatus {
        OPEN, REVIEWED, CLOSED
    }
}
