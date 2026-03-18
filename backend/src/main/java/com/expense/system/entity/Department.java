package com.expense.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "meals_limit")
    private BigDecimal mealsLimit;

    @Column(name = "travel_limit")
    private BigDecimal travelLimit;

    @Column(name = "accommodation_limit")
    private BigDecimal accommodationLimit;

    @Column(name = "office_supplies_limit")
    private BigDecimal officeSuppliesLimit;

    @Column(name = "entertainment_limit")
    private BigDecimal entertainmentLimit;

    @Column(name = "medical_limit")
    private BigDecimal medicalLimit;

    @Column(name = "single_expense_block_limit")
    private BigDecimal singleExpenseBlockLimit;

    @Column(name = "monthly_budget")
    private BigDecimal monthlyBudget;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
