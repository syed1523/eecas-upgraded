package com.expense.system.scheduler;

import com.expense.system.entity.Expense;
import com.expense.system.repository.ExpenseRepository;
import com.expense.system.repository.SystemConfigurationRepository;
import com.expense.system.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class EscalationJobScheduler {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private SystemConfigurationRepository configRepository;

    @Autowired
    private AuditService auditService;

    // Run every hour. For testing, we can run it every minute or have a manual
    // trigger.
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void executeEscalationJob() {
        int slaDays = configRepository.findByConfigKey("SLA_DAYS_THRESHOLD")
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(3);

        LocalDate cutoffDate = LocalDate.now().minusDays(slaDays);

        List<Expense> overdueExpenses = expenseRepository.findByStatusInAndExpenseDateBefore(
                List.of("SUBMITTED", "PENDING_MANAGER"), cutoffDate);

        for (Expense exp : overdueExpenses) {
            String oldStatus = exp.getStatus();
            exp.setStatus("ESCALATED");
            expenseRepository.save(exp);

            auditService.log(
                    "AUTO_ESCALATION",
                    "Expense",
                    exp.getId(),
                    "SYSTEM",
                    "SYSTEM",
                    oldStatus,
                    "ESCALATED",
                    "Expense escalated due to SLA breach (" + slaDays + " days)");
        }

        if (!overdueExpenses.isEmpty()) {
            System.out.println("[Scheduler] Escalated " + overdueExpenses.size() + " overdue expenses.");
        }
    }
}
