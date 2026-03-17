package com.expense.system.service;

import com.expense.system.dto.ExpenseDTO;
import com.expense.system.entity.Expense;
import java.util.List;
import java.util.Map;

public interface ExpenseService {
    Expense submitExpense(ExpenseDTO expenseDTO, String username);

    Expense saveDraft(ExpenseDTO expenseDTO, String username);

    List<Expense> getExpensesByUser(String username);

    org.springframework.data.domain.Page<Expense> getExpensesByUser(String username,
            org.springframework.data.domain.Pageable pageable);

    List<Expense> getAllExpenses(String username);

    List<Expense> getAllExpenses();

    org.springframework.data.domain.Page<Expense> getAllExpenses(org.springframework.data.domain.Pageable pageable);

    Expense getExpenseById(Long id);

    Expense acknowledgeExpense(Long id, String username);

    Expense updateExpenseStatus(Long id, String status, String comments, String username);

    Map<String, Object> getDashboardSummary(String username);
}
