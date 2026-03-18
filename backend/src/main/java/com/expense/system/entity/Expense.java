package com.expense.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Entity
@Table(name = "expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String title;

    @Column(length = 500)
    private String description;

    private BigDecimal amount;

    @Builder.Default
    @Column(columnDefinition = "VARCHAR(3) DEFAULT 'INR'")
    private String currency = "INR";

    private LocalDate expenseDate;

    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "department_name", length = 100)
    @Builder.Default
    private String departmentName = "";

    private String project;

    private String receiptPath;

    @Column(unique = true)
    private String receiptHash; // For duplicate detection

    @Column(length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({
            "hibernateLazyInitializer", "handler", "expenses", "password"
    })
    private User user;

    private boolean flagged;

    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "first_approver_id")
    private Long firstApproverId;

    @Column(columnDefinition = "TEXT")
    private String violationDetails;

    // Phase 9 - Intelligent Components Integration (System B)
    @Builder.Default
    private Boolean ocrMismatch = false;
    @Builder.Default
    private Boolean fraudIndicator = false;
    private BigDecimal ocrExtractedAmount;
    private String ocrExtractedMerchant;

    // Statistical Anomaly Detection
    private Double anomalyScore;

    @Column(name = "is_anomaly")
    @Builder.Default
    private Boolean isAnomaly = false;
    private String anomalyReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Explainable Flagging
    @Column(name = "flag_reasons", length = 1000)
    private String flagReasons;

    @Column(name = "flag_count")
    @Builder.Default
    private Integer flagCount = 0;

    @Column(name = "risk_level")
    @Builder.Default
    private String riskLevel = "LOW";

    // Phase 4 - Acknowledgment Loop
    private LocalDateTime acknowledgmentTimestamp;

    // Phase 6 - Payment Integration
    @Column(name = "payment_status")
    private String paymentStatus;

    private LocalDateTime paymentTimestamp;
    private String paymentReferenceId;
    private Long paidByUserId;

    // Phase 5 - Immutability State Tracking
    @Transient
    private String dbStatus;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @PostLoad
    public void postLoad() {
        this.dbStatus = this.status;
    }

    @PreUpdate
    public void preUpdate() {
        if ("PAID".equals(this.dbStatus) || "ARCHIVED".equals(this.dbStatus)) {
            throw new IllegalStateException(
                    "SOX Compliance Violation: Cannot modify a PAID or ARCHIVED expense record.");
        }
    }

    // SOX / IFRS Immutability — once set, record is locked from edits
    private LocalDateTime approvedAt;

    // VAT / GDPR — stores original file metadata as JSON
    @Column(columnDefinition = "TEXT")
    private String receiptMetadata;

    // Policy — required when status is REQUIRES_EXPLANATION
    @Column(columnDefinition = "TEXT")
    private String explanation;
}
