package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "evaluation_json", columnDefinition = "TEXT")
    private String evaluationJson;

    // Action: BLOCK, FLAG, WARN
    @Builder.Default
    @Column(name = "action", nullable = false)
    private String action = "FLAG";

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
