package com.expense.system.service;

import com.expense.system.entity.Expense;

public interface ReceiptProcessingService {
    void processReceipt(Expense expense);
}
