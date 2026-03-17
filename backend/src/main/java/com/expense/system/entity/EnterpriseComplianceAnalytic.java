package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "enterprise_compliance_analytics", indexes = {
        @Index(name = "idx_enterprise_date", columnList = "recordDate")
})
@Data
public class EnterpriseComplianceAnalytic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private LocalDate recordDate;

    private Double complianceScore;
    private Double escalationRate;
    private Double overrideRate;
    private Double investigationClosureRate;
    private Double riskIndex;

    private Integer totalViolations;
    private Integer totalInvestigations;
}
