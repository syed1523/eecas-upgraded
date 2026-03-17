package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "manager_performance_metrics", indexes = {
        @Index(name = "idx_manager_date", columnList = "managerUserId, recordDate")
})
@Data
public class ManagerPerformanceMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private Long managerUserId;
    private String departmentName;
    private LocalDate recordDate;

    private Double avgLatencyHours;
    private Double overrideFrequency;
}
