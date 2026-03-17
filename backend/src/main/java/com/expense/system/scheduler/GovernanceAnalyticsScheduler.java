package com.expense.system.scheduler;

import com.expense.system.service.GovernanceAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class GovernanceAnalyticsScheduler {

    private final GovernanceAnalyticsService analyticsService;

    // Runs every day at 1:00 AM
    // Using simple approach here, but in production, use @SchedulerLock(name =
    // "analyticsScheduler", lockAtMostFor = "10m")
    @Scheduled(cron = "0 0 1 * * ?")
    public void runDailyAnalytics() {
        log.info("Triggering Scheduled Daily Governance Analytics...");
        try {
            // Aggregate for the *previous* day's completed data boundaries
            LocalDate targetDate = LocalDate.now().minusDays(1);
            analyticsService.generateDailyAnalytics(targetDate);
        } catch (Exception e) {
            log.error("Failed to run daily governance analytics: ", e);
        }
    }
}
