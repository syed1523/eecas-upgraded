package com.expense.system.service.impl;

import com.expense.system.entity.Budget;
import com.expense.system.repository.BudgetRepository;
import com.expense.system.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class BudgetServiceImpl implements BudgetService {
    @Autowired
    private BudgetRepository budgetRepository;

    @Override
    public Budget createBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    @Override
    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    @Override
    public boolean checkBudgetAvailability(String department, BigDecimal amount) {
        return budgetRepository.findByDepartment(department)
                .map(budget -> {
                    BigDecimal projectedUsage = budget.getUsedAmount().add(amount);
                    return projectedUsage.compareTo(budget.getTotalAmount()) <= 0;
                })
                .orElse(true); // If no budget defined, assume unlimited? Or block?
                               // Requirement says "Set department monthly budget".
                               // If not set, let's assume it's allowed for now or return false.
                               // Let's return true to avoid blocking if admin hasn't set it yet.
    }

    @Override
    @Transactional
    public void updateBudgetUsage(String department, BigDecimal amount) {
        budgetRepository.findByDepartment(department).ifPresent(budget -> {
            budget.setUsedAmount(budget.getUsedAmount().add(amount));
            budgetRepository.save(budget);
        });
    }

    @Override
    @Transactional
    public void refundBudget(String department, BigDecimal amount) {
        budgetRepository.findByDepartment(department).ifPresent(budget -> {
            BigDecimal newUsage = budget.getUsedAmount().subtract(amount);
            if (newUsage.compareTo(BigDecimal.ZERO) < 0) {
                newUsage = BigDecimal.ZERO;
            }
            budget.setUsedAmount(newUsage);
            budgetRepository.save(budget);
        });
    }

    @Override
    public Page<Budget> findAll(Pageable pageable) {
        return budgetRepository.findAll(pageable);
    }
}
