package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable record of every manager policy override.
 * Append-only — no update/delete operations exposed at service level.
 */
@Entity
@Table(name = "override_logs")
@Data
public class OverrideLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long expenseId;
    private Long managerId;
    private String managerUsername;
    private String department;

    // What policy was violated
    private String ruleViolated;
    private BigDecimal thresholdExceeded;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String justification;

    private String ipAddress;
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
