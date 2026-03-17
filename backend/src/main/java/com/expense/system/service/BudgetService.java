package com.expense.system.service;

import com.expense.system.entity.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;

public interface BudgetService {
    Budget createBudget(Budget budget);

    List<Budget> getAllBudgets();

    boolean checkBudgetAvailability(String department, BigDecimal amount);

    void updateBudgetUsage(String department, BigDecimal amount);

    void refundBudget(String department, BigDecimal amount);

    Page<Budget> findAll(Pageable pageable);
}
