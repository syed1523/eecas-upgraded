package com.expense.system.service.impl;

import com.expense.system.entity.Expense;
import com.expense.system.service.ReceiptProcessingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ReceiptProcessingServiceImpl implements ReceiptProcessingService {

    @Override
    public void processReceipt(Expense expense) {
        if (expense.getReceiptPath() == null || expense.getReceiptPath().isBlank()) {
            return;
        }

        // Mock OCR extraction (simulating System B's OCR logic)
        // In a real scenario, this would call an external Python service or Python
        // shell script

        // System B logic simulation:
        // Returns {"text": text, "amount": None} or similar.

        // For simulation purposes: extract amount as 90% of actual, if amount > 0
        if (expense.getAmount() != null && expense.getAmount().compareTo(BigDecimal.ZERO) > 0) {

            // Simulating OCR parsing errors to test UI (triggering for amount < 100)
            if (expense.getAmount().compareTo(new BigDecimal("100")) < 0) {
                expense.setOcrExtractedAmount(expense.getAmount().multiply(new BigDecimal("0.9"))); // OCR missed
                                                                                                    // something
                expense.setOcrMismatch(true);
            } else {
                expense.setOcrExtractedAmount(expense.getAmount());
                expense.setOcrMismatch(false);
            }
        }

        expense.setOcrExtractedMerchant("ACME Corp"); // Mock extracted merchant
    }
}
