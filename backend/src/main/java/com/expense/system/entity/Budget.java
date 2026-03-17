package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private String department;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Builder.Default
    private BigDecimal usedAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal spendingLimit; // Threshold for warning (e.g. 80%)

    private LocalDate startDate;
    private LocalDate endDate;
}
