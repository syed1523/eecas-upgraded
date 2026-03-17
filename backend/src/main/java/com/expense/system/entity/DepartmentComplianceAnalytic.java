package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "department_compliance_analytics", indexes = {
        @Index(name = "idx_dept_date", columnList = "departmentName, recordDate")
})
@Data
public class DepartmentComplianceAnalytic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String departmentName;
    private LocalDate recordDate;

    private Double complianceScore;
    private Double overrideRate;
    private Double escalationRate;
    private Double avgApprovalTimeHours;

    private Integer totalViolations;
    private Integer totalInvestigations;
}
