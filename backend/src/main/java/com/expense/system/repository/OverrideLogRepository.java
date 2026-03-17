package com.expense.system.repository;

import com.expense.system.entity.OverrideLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OverrideLogRepository extends JpaRepository<OverrideLog, Long> {
    List<OverrideLog> findByExpenseId(Long expenseId);

    List<OverrideLog> findByManagerUsername(String managerUsername);

    List<OverrideLog> findByDepartment(String department);
}
