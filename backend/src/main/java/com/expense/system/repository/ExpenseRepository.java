package com.expense.system.repository;

import com.expense.system.entity.Expense;
import com.expense.system.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Expense> {
        List<Expense> findByStatus(String status);

        List<Expense> findByStatusIn(List<String> statuses);

        org.springframework.data.domain.Page<Expense> findByStatusIn(List<String> statuses,
                        org.springframework.data.domain.Pageable pageable);

        List<Expense> findByUserId(Long userId);

        org.springframework.data.domain.Page<Expense> findByUserId(Long userId,
                        org.springframework.data.domain.Pageable pageable);

        List<Expense> findByUserDepartmentId(Long departmentId);

        List<Expense> findByUserDepartmentIdAndStatus(Long departmentId, String status);

        List<Expense> findByUserDepartmentIdAndStatusIn(Long departmentId, List<String> statuses);

        @Query("SELECT e FROM Expense e WHERE e.user.department = :dept")
        List<Expense> findByDepartment(@Param("dept") Department dept);

        @Query("SELECT e FROM Expense e WHERE e.user.department = :dept")
        org.springframework.data.domain.Page<Expense> findByDepartment(@Param("dept") Department dept,
                        org.springframework.data.domain.Pageable pageable);

        // Supporting methods for existing logic
        List<Expense> findByIsAnomalyTrue();

        List<Expense> findByUserIdAndCategoryAndCreatedAtAfterAndIdNot(Long userId, String category,
                        java.time.LocalDateTime createdAt, long id);

        List<Expense> findByStatusInAndExpenseDateBefore(List<String> statuses, java.time.LocalDate date);

        org.springframework.data.domain.Page<Expense> findByFlagCountGreaterThan(int flagCount,
                        org.springframework.data.domain.Pageable pageable);

        org.springframework.data.domain.Page<Expense> findByIsAnomalyTrue(
                        org.springframework.data.domain.Pageable pageable);

        org.springframework.data.domain.Page<Expense> findByRiskLevelIn(List<String> riskLevels,
                        org.springframework.data.domain.Pageable pageable);

        org.springframework.data.domain.Page<Expense> findByStatus(String status,
                        org.springframework.data.domain.Pageable pageable);
}
