package com.expense.system.repository;

import com.expense.system.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByDepartment(String department);
}
