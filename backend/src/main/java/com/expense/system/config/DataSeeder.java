package com.expense.system.config;

import com.expense.system.entity.Approval;
import com.expense.system.entity.AuditLog;
import com.expense.system.entity.ComplianceRule;
import com.expense.system.entity.Department;
import com.expense.system.entity.ERole;
import com.expense.system.entity.Expense;
import com.expense.system.entity.Role;
import com.expense.system.entity.User;
import com.expense.system.repository.ApprovalRepository;
import com.expense.system.repository.AuditLogRepository;
import com.expense.system.repository.ComplianceRuleRepository;
import com.expense.system.repository.DepartmentRepository;
import com.expense.system.repository.ExpenseRepository;
import com.expense.system.repository.RoleRepository;
import com.expense.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final ExpenseRepository expenseRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApprovalRepository approvalRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (expenseRepository.count() < 10) {
            System.out.println("--- AEGIS DATA SEEDING INITIATED ---");

            Department eng = ensureDepartment("Engineering", "Core Product Development",
                    "800", "5000", "4000", "2000", "1000", "5000", "10000", "100000");
            Department mkt = ensureDepartment("Marketing", "Growth and Brand",
                    "1200", "8000", "6000", "1500", "5000", "5000", "15000", "200000");
            Department hr = ensureDepartment("HR", "People and Culture",
                    "600", "3000", "3000", "1000", "2000", "5000", "5000", "50000");
            Department fin = ensureDepartment("Finance", "Finance and Audit",
                    "500", "2000", "2000", "500", "500", "5000", "5000", "30000");

            seedRoles();

            User admin = ensureUser("admin@test.com", "password", "Admin User", "ADMIN", fin, "System Admin",
                    "EMP-000",
                    ERole.ROLE_ADMIN);

            User ravi = ensureUser("employee1@test.com", "password", "Ravi Kumar", "EMPLOYEE", eng, "Senior Dev",
                    "EMP-101", ERole.ROLE_EMPLOYEE);
            User arjun = ensureUser("employee2@test.com", "password", "Arjun Singh", "EMPLOYEE", eng, "QA Engineer",
                    "EMP-102", ERole.ROLE_EMPLOYEE);
            User sneha = ensureUser("employee3@test.com", "password", "Sneha Nair", "EMPLOYEE", eng, "DevOps",
                    "EMP-103", ERole.ROLE_EMPLOYEE);
            User sunita = ensureUser("manager1@test.com", "password", "Sunita Patel", "MANAGER", eng,
                    "Engineering Manager", "EMP-M1", ERole.ROLE_MANAGER);

            User divya = ensureUser("employee4@test.com", "password", "Divya Krishnan", "EMPLOYEE", mkt,
                    "Brand Manager",
                    "EMP-201", ERole.ROLE_EMPLOYEE);
            User karan = ensureUser("employee5@test.com", "password", "Karan Mehta", "EMPLOYEE", mkt,
                    "SEO Specialist",
                    "EMP-202", ERole.ROLE_EMPLOYEE);
            User raj = ensureUser("manager2@test.com", "password", "Raj Mehta", "MANAGER", mkt, "Marketing Head",
                    "EMP-M2", ERole.ROLE_MANAGER);

            User pooja = ensureUser("employee6@test.com", "password", "Pooja Iyer", "EMPLOYEE", hr, "HR Recruiter",
                    "EMP-301", ERole.ROLE_EMPLOYEE);
            User anita = ensureUser("manager3@test.com", "password", "Anita Sharma", "MANAGER", hr, "HR Director",
                    "EMP-M3", ERole.ROLE_MANAGER);

            User deepak = ensureUser("finance1@test.com", "password", "Deepak Menon", "FINANCE", fin,
                    "Finance Analyst",
                    "EMP-F1", ERole.ROLE_FINANCE);
            User priya = ensureUser("finance2@test.com", "password", "Priya Sharma", "FINANCE", fin,
                    "Tax Consultant",
                    "EMP-F2", ERole.ROLE_FINANCE);
            User kavitha = ensureUser("auditor1@test.com", "password", "Kavitha Rao", "AUDITOR", fin, "Risk Auditor",
                    "EMP-A1", ERole.ROLE_AUDITOR);

            ensureComplianceRule("Daily Food Limit", "Max 800 per day",
                    "{\"type\":\"limit\",\"category\":\"MEALS\",\"max\":800}", "FLAG");
            ensureComplianceRule("Weekend Submission", "Flag weekend submissions",
                    "{\"type\":\"weekend\"}", "FLAG");
            ensureComplianceRule("Alcohol Policy", "No entertainment above 5000",
                    "{\"type\":\"limit\",\"category\":\"ENTERTAINMENT\",\"max\":5000}", "BLOCK");
            ensureComplianceRule("Distance Check", "Travel must be above 50km",
                    "{\"type\":\"custom\",\"logic\":\"travelDist > 50\"}", "WARN");
            ensureComplianceRule("Duplicate Receipt", "Same hash check",
                    "{\"type\":\"duplicate\"}", "BLOCK");
            ensureComplianceRule("Late Night Travel", "Travel after 10PM",
                    "{\"type\":\"time\",\"after\":\"22:00\"}", "FLAG");
            ensureComplianceRule("Medical Limit", "Annual medical cap",
                    "{\"type\":\"limit\",\"category\":\"MEDICAL\",\"max\":15000}", "FLAG");

            seedOriginalScenarios(admin, ravi, arjun, sneha, sunita, divya, karan, raj, pooja, anita, deepak, priya,
                    kavitha);

            int newExpensesAdded = seedExpandedExpenses(admin, ravi, arjun, sneha, sunita, divya, karan, raj, pooja,
                    anita, deepak, priya);

            System.out.println("--- AEGIS DATA SEEDING COMPLETE ---");
            System.out.println("Original scenarios preserved: 17");
            System.out.println("Expanded realistic expenses added: " + newExpensesAdded);
            System.out.println("Departments: 4");

            verifyAccountExistence();
            return;
        }

        if (expenseRepository.count() >= 10) {
            System.out.println("--- EXPENSE DATA ALREADY SEEDED, SKIPPING DATASEEDER ---");
            verifyAccountExistence();
            return;
        }
    }

    private void seedOriginalScenarios(User admin, User ravi, User arjun, User sneha, User sunita, User divya,
            User karan, User raj, User pooja, User anita, User deepak, User priya, User kavitha) {
        Expense s1e1 = save(buildExpense(ravi, "AWS Certification", bd("12000"), "OFFICE_SUPPLIES", "APPROVED",
                "Cloud training", LocalDateTime.now().minusDays(10), false, 0, "LOW", 0, "", "Engineering"));
        logExpense(s1e1, "SUBMITTED", ravi, "USER", "Initial submission",
                LocalDateTime.now().minusDays(10).plusHours(1));
        createApprovalRecord(s1e1, sunita, LocalDateTime.now().minusDays(9), "APPROVED", "Manager Approval", null);
        logExpense(s1e1, "APPROVED", sunita, "MANAGER", "Manager Approval", LocalDateTime.now().minusDays(9));
        logExpense(s1e1, "PAID", deepak, "FINANCE", "Payment processed", LocalDateTime.now().minusDays(8));

        Expense s2e1 = save(buildExpense(ravi, "Team Pizza", bd("5500"), "MEALS", "REJECTED",
                "Monthly hangout | Rejection reason: exceeds daily meal limit", LocalDateTime.now().minusDays(15),
                true, 2.5, "MEDIUM", 1, "Exceeds daily meal limit", "Engineering"));
        logExpense(s2e1, "SUBMITTED", ravi, "USER", "Submission", LocalDateTime.now().minusDays(15));
        logExpense(s2e1, "REJECTED", sunita, "MANAGER", "Exceeds department budget for snacks",
                LocalDateTime.now().minusDays(14));

        Expense s3e1 = save(buildExpense(divya, "Client Lunch", bd("8500"), "ENTERTAINMENT", "UNDER_REVIEW",
                "High-end restaurant", LocalDateTime.now().minusDays(2), true, 4.2, "CRITICAL", 3,
                "Statistical Anomaly; Outlier Amount; Pattern Flag", "Marketing"));
        logExpense(s3e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(2));
        logExpense(s3e1, "FLAGGED", null, "SYSTEM", "Statistical Anomaly Detected (Z-score 4.2)",
                LocalDateTime.now().minusDays(2).plusMinutes(1));

        Expense s4e1 = save(buildExpense(ravi, "Internet Bill", bd("1500"), "UTILITIES", "PENDING",
                "Monthly reimbursement", LocalDateTime.now().minusHours(12), false, 0, "LOW", 0, "",
                "Engineering"));
        logExpense(s4e1, "SUBMITTED", ravi, "USER", "Submission", LocalDateTime.now().minusHours(12));

        save(buildExpense(sneha, "Monitor Stand", bd("2500"), "OFFICE_SUPPLIES", "DRAFT",
                "Ergonomics", LocalDateTime.now().minusHours(24), false, 0, "LOW", 0, "", "Engineering"));

        Expense s6e1 = save(buildExpense(karan, "Business Trip Uber", bd("450"), "TRAVEL", "PENDING",
                "Airport drop", LocalDateTime.now().minusDays(1), false, 0, "MEDIUM", 2,
                "Duplicate merchant pattern; Frequent travel", "Marketing"));
        logExpense(s6e1, "SUBMITTED", karan, "USER", "Submission", LocalDateTime.now().minusDays(1));

        Expense s7e1 = save(buildExpense(divya, "Booth Rental", bd("25000"), "TRAVEL", "PENDING",
                "Event booking", LocalDateTime.now().minusDays(1), false, 1.2, "MEDIUM", 1,
                "Weekend Submission", "Marketing"));
        logExpense(s7e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(1));

        Expense s8e1 = save(buildExpense(sunita, "Offsite Venue", bd("45000"), "TRAVEL", "PENDING",
                "Team retreat", LocalDateTime.now(), false, 0, "LOW", 0, "", "Engineering"));
        logExpense(s8e1, "SUBMITTED", sunita, "MANAGER", "Submission", LocalDateTime.now());

        Expense s9e1 = save(buildExpense(arjun, "Medical Checkup", bd("2200"), "MEDICAL", "APPROVED",
                "Annual check", LocalDateTime.now().minusDays(20), true, 0, "LOW", 1,
                "Statistical Anomaly", "Engineering"));
        logExpense(s9e1, "SUBMITTED", arjun, "USER", "Submission", LocalDateTime.now().minusDays(20));
        logExpense(s9e1, "AUDIT_REVIEW", kavitha, "AUDITOR", "Manual verification pending",
                LocalDateTime.now().minusDays(19));
        logExpense(s9e1, "CLEARED", kavitha, "AUDITOR", "Valid medical claim after review",
                LocalDateTime.now().minusDays(18));
        createApprovalRecord(s9e1, sunita, LocalDateTime.now().minusDays(17), "APPROVED", "Manager Approval", null);
        logExpense(s9e1, "APPROVED", sunita, "MANAGER", "Manager Approval", LocalDateTime.now().minusDays(17));

        Expense s10e1 = save(buildExpense(pooja, "Job Portal Ad", bd("12000"), "OFFICE_SUPPLIES", "PENDING",
                "LinkedIn hiring", LocalDateTime.now().minusDays(3), false, 0, "LOW", 0, "", "HR"));
        logExpense(s10e1, "SUBMITTED", pooja, "USER", "Submission", LocalDateTime.now().minusDays(3));

        Expense s11e1 = save(buildExpense(divya, "Hotel Stay", bd("12500"), "ACCOMMODATION", "PENDING",
                "Conference", LocalDateTime.now().minusDays(5), false, 0, "LOW", 0, "", "Marketing"));
        logExpense(s11e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(5));

        Expense s12e1 = save(buildExpense(karan, "Alcohol and Party", bd("50000"), "ENTERTAINMENT", "REJECTED",
                "Team party | Rejection reason: policy violation", LocalDateTime.now().minusDays(40), true, 6.5,
                "CRITICAL", 5, "Policy Violation: Alcohol; High Amount; Pattern Suspect", "Marketing"));
        logExpense(s12e1, "SUBMITTED", karan, "USER", "Submission", LocalDateTime.now().minusDays(40));
        logExpense(s12e1, "REJECTED", raj, "MANAGER", "Policy violation", LocalDateTime.now().minusDays(39));

        Expense s13e1 = save(buildExpense(sunita, "Flight to London", bd("65000"), "TRAVEL", "PENDING",
                "Quarterly planning", LocalDateTime.now().minusHours(24), false, 0, "LOW", 0, "",
                "Engineering"));
        logExpense(s13e1, "SUBMITTED", sunita, "MANAGER", "Submission", LocalDateTime.now().minusHours(24));

        Expense s14e1 = save(buildExpense(anita, "Leadership Workshop", bd("35000"), "OFFICE_SUPPLIES",
                "APPROVED", "External training", LocalDateTime.now().minusDays(60), false, 0, "LOW", 0,
                "", "HR"));
        logExpense(s14e1, "SUBMITTED", anita, "MANAGER", "Submission", LocalDateTime.now().minusDays(60));
        createApprovalRecord(s14e1, admin, LocalDateTime.now().minusDays(59), "APPROVED", "Admin auto-approval",
                null);
        logExpense(s14e1, "APPROVED", admin, "ADMIN", "Admin auto-approval", LocalDateTime.now().minusDays(59));

        Expense s15e1 = save(buildExpense(divya, "Notebooks", bd("250"), "OFFICE_SUPPLIES", "PAID",
                "Stationary", LocalDateTime.now().minusDays(30), false, 0, "LOW", 0, "", "Marketing"));
        logExpense(s15e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(30));
        createApprovalRecord(s15e1, raj, LocalDateTime.now().minusDays(29), "APPROVED", "Manager Approval", null);
        logExpense(s15e1, "APPROVED", raj, "MANAGER", "Manager Approval", LocalDateTime.now().minusDays(29));
        logExpense(s15e1, "PAID", priya, "FINANCE", "Payment processed", LocalDateTime.now().minusDays(28));

        Expense s16e1 = save(buildExpense(divya, "Urgent Courier", bd("1200"), "TRAVEL", "APPROVED",
                "Client docs", LocalDateTime.now().minusDays(4), true, 2.8, "MEDIUM", 1,
                "Amount outlier", "Marketing"));
        logExpense(s16e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(4));
        logExpense(s16e1, "AUDIT_REVIEW", kavitha, "AUDITOR", "Verifying receipt",
                LocalDateTime.now().minusDays(3));
        logExpense(s16e1, "CLEARED", kavitha, "AUDITOR", "Approved after review",
                LocalDateTime.now().minusDays(2));
        createApprovalRecord(s16e1, raj, LocalDateTime.now().minusDays(2).plusHours(4), "APPROVED",
                "Approved after audit review", null);
        logExpense(s16e1, "APPROVED", raj, "MANAGER", "Approved after audit review",
                LocalDateTime.now().minusDays(2).plusHours(4));

        Expense s17e1 = save(buildExpense(pooja, "Pharma Bill", bd("500"), "MEDICAL", "PENDING", "Medicine",
                LocalDateTime.now().minusHours(1), false, 0, "LOW", 0, "", "HR"));
        logExpense(s17e1, "SUBMITTED", pooja, "USER", "Submission", LocalDateTime.now().minusHours(1));
    }

    private int seedExpandedExpenses(User admin, User ravi, User arjun, User sneha, User sunita, User divya,
            User karan, User raj, User pooja, User anita, User deepak, User priya) {
        int added = 0;

        added += seedRequestedExpense(ravi, sunita, "Uber to client site", "Travel reimbursement to client site",
                "TRAVEL", "850", "APPROVED", 5, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "Team lunch", "Engineering team lunch with sprint review",
                "MEALS", "2400", "APPROVED", 12, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "IntelliJ license renewal", "IDE renewal for development work",
                "SOFTWARE", "8500", "APPROVED", 20, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "Flight to Bangalore office",
                "Travel to Bangalore office for architecture workshop", "TRAVEL", "6200", "APPROVED", 35,
                false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "Hotel Bangalore 2 nights",
                "Accommodation during Bangalore office visit", "ACCOMMODATION", "7800", "APPROVED", 34,
                false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "USB-C hub purchase", "Peripheral purchase for workstation",
                "EQUIPMENT", "3200", "APPROVED", 50, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "Client dinner", "Dinner with client during engineering demo",
                "MEALS", "4500", "APPROVED", 60, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "Ola cab airport", "Airport drop for office travel",
                "TRAVEL", "1200", "APPROVED", 70, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "GitHub Copilot subscription", "Developer tooling subscription",
                "SOFTWARE", "9000", "PENDING", 2, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "Metro card recharge", "Commute recharge for office travel",
                "TRAVEL", "500", "PENDING", 1, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(ravi, sunita, "Mechanical keyboard", "High-end keyboard for productivity",
                "EQUIPMENT", "12000", "PENDING", 3, false, null, "HIGH", 1, "HIGH_AMOUNT", null);
        added += seedRequestedExpense(ravi, sunita, "Working lunch", "Working lunch during release day",
                "MEALS", "800", "DRAFT", 0, false, null, "LOW", 0, "", null);

        added += seedRequestedExpense(arjun, sunita, "Auto to office", "Local commute to office campus",
                "TRAVEL", "180", "APPROVED", 8, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Postman Pro", "API tooling subscription for QA work",
                "SOFTWARE", "4200", "APPROVED", 15, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Laptop stand", "Ergonomic setup for QA desk",
                "EQUIPMENT", "2100", "APPROVED", 22, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Team outing", "Engineering and QA team outing meal",
                "MEALS", "3600", "APPROVED", 30, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Cab to airport", "Airport cab for client-site travel",
                "TRAVEL", "950", "APPROVED", 45, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Hotel Chennai", "Accommodation during Chennai QA workshop",
                "ACCOMMODATION", "5500", "APPROVED", 44, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Udemy course purchase", "Online upskilling course purchase",
                "TRAINING", "3800", "APPROVED", 55, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Monitor arm", "Desk setup improvement for second display",
                "EQUIPMENT", "4500", "APPROVED", 65, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Docker Desktop license",
                "Software license for container testing", "SOFTWARE", "6000", "PENDING", 4, false, null,
                "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Department birthday cake", "Cake for engineering celebration",
                "MEALS", "1200", "PENDING", 2, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(arjun, sunita, "Taxi to client meeting", "Client meeting commute by taxi",
                "TRAVEL", "750", "REJECTED", 18, false, null, "LOW", 0, "", "no receipt");
        added += seedRequestedExpense(arjun, sunita, "Second monitor", "Additional display purchase request",
                "EQUIPMENT", "18000", "REJECTED", 40, true, 3.2, "HIGH", 0, "", "exceeds limit");

        added += seedRequestedExpense(sneha, sunita, "Lunch with vendor", "Vendor lunch during infra discussion",
                "MEALS", "2800", "APPROVED", 10, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(sneha, sunita, "Flight Mumbai", "Travel to Mumbai for deployment review",
                "TRAVEL", "8900", "APPROVED", 25, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(sneha, sunita, "Hotel Mumbai 3 nights",
                "Accommodation during Mumbai deployment visit", "ACCOMMODATION", "12000", "APPROVED", 24,
                false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(sneha, sunita, "Webcam for remote calls", "Equipment for hybrid meetings",
                "EQUIPMENT", "3500", "APPROVED", 38, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(sneha, sunita, "Figma subscription", "Design collaboration subscription",
                "SOFTWARE", "5200", "APPROVED", 48, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(sneha, sunita, "AWS certification exam fee",
                "Certification exam fee for cloud architecture", "TRAINING", "15000", "APPROVED", 58,
                false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(sneha, sunita, "Cab to training center",
                "Local travel for certification training", "TRAVEL", "420", "APPROVED", 57, false, null,
                "LOW", 0, "", null);
        added += seedRequestedExpense(sneha, sunita, "Working dinner", "Late evening deployment dinner",
                "MEALS", "1900", "PENDING", 5, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(sneha, sunita, "Noise cancelling headphones",
                "Headphones for operations bridge calls", "EQUIPMENT", "22000", "PENDING", 1, true, 3.8,
                "CRITICAL", 2, "HIGH_AMOUNT,UNUSUAL_CATEGORY", null);
        added += seedRequestedExpense(sneha, sunita, "Weekend office cab", "Weekend cab for urgent release support",
                "TRAVEL", "680", "PENDING", 0, false, null, "MEDIUM", 1, "WEEKEND_SUBMISSION", null);

        added += seedRequestedExpense(divya, raj, "Client entertainment", "Client entertainment during campaign pitch",
                "ENTERTAINMENT", "8500", "APPROVED", 7, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Flight to Delhi for campaign",
                "Travel to Delhi for campaign launch", "TRAVEL", "11200", "APPROVED", 20, false, null,
                "LOW", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Hotel Delhi 2 nights",
                "Accommodation during Delhi campaign visit", "ACCOMMODATION", "9800", "APPROVED", 19,
                false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Banner printing", "Campaign collateral printing expense",
                "MARKETING", "6400", "APPROVED", 28, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Influencer lunch meeting",
                "Lunch meeting with influencer partners", "MEALS", "5200", "APPROVED", 35, false, null,
                "LOW", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Product launch dinner", "Launch dinner with media and clients",
                "ENTERTAINMENT", "14000", "APPROVED", 50, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Cab for campaign shoot", "Local travel for campaign shoot",
                "TRAVEL", "1800", "APPROVED", 55, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Social media ads", "Digital campaign ad spend",
                "MARKETING", "25000", "APPROVED", 60, true, 2.4, "HIGH", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Client golf event", "Client engagement event expense",
                "ENTERTAINMENT", "18000", "PENDING", 3, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Flight to Hyderabad", "Campaign travel to Hyderabad",
                "TRAVEL", "9500", "PENDING", 2, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(divya, raj, "Promotional merchandise", "Bulk merchandise for product campaign",
                "MARKETING", "32000", "PENDING", 1, true, 2.8, "HIGH", 2, "HIGH_AMOUNT,ROUND_AMOUNT", null);
        added += seedRequestedExpense(divya, raj, "Team celebration", "Marketing team celebration meal",
                "MEALS", "4800", "REJECTED", 22, false, null, "LOW", 0, "", "policy exception");

        added += seedRequestedExpense(karan, raj, "Campaign design agency", "External creative agency invoice",
                "MARKETING", "45000", "APPROVED", 15, true, 3.1, "CRITICAL", 0, "", null);
        added += seedRequestedExpense(karan, raj, "Cab to photo shoot", "Local travel to campaign shoot location",
                "TRAVEL", "2200", "APPROVED", 18, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(karan, raj, "Brand strategy lunch", "Lunch with brand strategy partner",
                "MEALS", "3800", "APPROVED", 25, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(karan, raj, "Award ceremony tickets", "Industry award event attendance",
                "ENTERTAINMENT", "12000", "APPROVED", 40, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(karan, raj, "Auto for office errands", "Short travel for office errands",
                "TRAVEL", "350", "APPROVED", 45, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(karan, raj, "Email marketing tool", "Subscription for campaign mailing",
                "MARKETING", "8800", "APPROVED", 55, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(karan, raj, "Hotel Pune", "Accommodation during Pune campaign review",
                "ACCOMMODATION", "6200", "APPROVED", 62, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(karan, raj, "Content creation tools", "Creative tooling subscription renewal",
                "MARKETING", "15000", "PENDING", 4, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(karan, raj, "Outstation cab", "Intercity cab for vendor coordination",
                "TRAVEL", "3400", "PENDING", 2, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(karan, raj, "Team outing event", "Department outing entertainment expense",
                "ENTERTAINMENT", "22000", "PENDING", 1, false, null, "LOW", 0, "", null);

        added += seedRequestedExpense(pooja, anita, "HR certification", "Professional certification for HR role",
                "TRAINING", "12000", "APPROVED", 10, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(pooja, anita, "Candidate interview lunch", "Interview panel lunch expense",
                "MEALS", "1800", "APPROVED", 15, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(pooja, anita, "Cab to recruitment fair", "Travel to recruitment fair venue",
                "TRAVEL", "650", "APPROVED", 20, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(pooja, anita, "SHRM conference fee", "Conference registration fee",
                "TRAINING", "8500", "APPROVED", 30, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(pooja, anita, "Onboarding team lunch", "Lunch for new joiner onboarding",
                "MEALS", "3200", "APPROVED", 38, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(pooja, anita, "Stationery for HR dept", "Office supplies for HR operations",
                "OFFICE_SUPPLIES", "2400", "APPROVED", 48, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(pooja, anita, "Auto to medical camp", "Travel to employee wellness camp",
                "TRAVEL", "280", "APPROVED", 55, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(pooja, anita, "Online HR course", "Upskilling course for HR processes",
                "TRAINING", "4500", "PENDING", 3, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(pooja, anita, "Exit interview lunch", "Lunch during exit interview session",
                "MEALS", "1400", "PENDING", 1, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(pooja, anita, "Printer cartridges", "Supplies request missing description",
                "OFFICE_SUPPLIES", "3800", "REJECTED", 25, false, null, "MEDIUM", 1, "MISSING_DESCRIPTION",
                "missing description");

        added += seedRequestedExpense(deepak, admin, "CFA exam fee", "Professional certification fee",
                "TRAINING", "18000", "APPROVED", 20, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(deepak, admin, "Tally ERP license", "Finance software license renewal",
                "SOFTWARE", "22000", "APPROVED", 35, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(deepak, admin, "Cab to CA office", "Local travel to chartered accountant office",
                "TRAVEL", "480", "APPROVED", 42, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(deepak, admin, "Audit team lunch", "Lunch meeting with internal audit team",
                "MEALS", "2600", "APPROVED", 50, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(deepak, admin, "Finance summit", "Conference attendance for finance summit",
                "TRAINING", "9500", "APPROVED", 60, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(deepak, admin, "Bloomberg terminal", "Premium finance terminal subscription",
                "SOFTWARE", "45000", "PENDING", 5, true, 2.9, "HIGH", 1, "HIGH_AMOUNT", null);
        added += seedRequestedExpense(deepak, admin, "Investor meeting dinner", "Dinner with investor relations team",
                "MEALS", "8800", "PENDING", 2, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(deepak, admin, "Flight to Mumbai audit", "Travel for external audit visit",
                "TRAVEL", "7200", "PENDING", 1, false, null, "LOW", 0, "", null);

        added += seedRequestedExpense(priya, admin, "QuickBooks subscription", "Accounting software subscription",
                "SOFTWARE", "6800", "APPROVED", 28, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(priya, admin, "Tax law seminar", "Seminar fee for tax law update",
                "TRAINING", "5500", "APPROVED", 40, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(priya, admin, "Client accounting lunch", "Lunch with accounting advisory client",
                "MEALS", "3200", "APPROVED", 52, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(priya, admin, "Cab to RBI office", "Travel to RBI office for filing work",
                "TRAVEL", "720", "APPROVED", 58, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(priya, admin, "Financial modeling tool", "Tool subscription for forecasting",
                "SOFTWARE", "12000", "PENDING", 3, false, null, "LOW", 0, "", null);
        added += seedRequestedExpense(priya, admin, "ICAI workshop", "Workshop fee for accounting standards update",
                "TRAINING", "4200", "PENDING", 1, false, null, "LOW", 0, "", null);

        return added;
    }

    private int seedRequestedExpense(User user, User approver, String title, String description, String category,
            String amount, String status, long daysAgo, boolean isAnomaly, Double anomalyScore, String riskLevel,
            Integer flagCount, String flagReasons, String rejectionReason) {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(daysAgo);
        String fullDescription = description;
        if (rejectionReason != null && !rejectionReason.isBlank()) {
            fullDescription = description + " | Rejection reason: " + rejectionReason;
        }

        Expense expense = save(buildExpense(user, title, bd(amount), category, status, fullDescription, createdAt,
                isAnomaly, anomalyScore != null ? anomalyScore : 0.0, riskLevel != null ? riskLevel : "LOW",
                flagCount != null ? flagCount : 0, flagReasons != null ? flagReasons : "",
                user.getDepartment() != null ? user.getDepartment().getName() : ""));

        if (!"DRAFT".equals(status)) {
            logExpense(expense, "SUBMITTED", user, user.getRole(), "Seeded submission", createdAt.plusMinutes(15));
        }

        if ("APPROVED".equals(status)) {
            LocalDateTime approvalTime = createdAt.plusHours(6);
            createApprovalRecord(expense, approver, approvalTime, "APPROVED", "Seeded approval", null);
            logExpense(expense, "APPROVED", approver, approver != null ? approver.getRole() : "SYSTEM",
                    "Seeded approval", approvalTime);
        } else if ("REJECTED".equals(status)) {
            LocalDateTime rejectedTime = createdAt.plusHours(8);
            logExpense(expense, "REJECTED", approver, approver != null ? approver.getRole() : "SYSTEM",
                    rejectionReason != null && !rejectionReason.isBlank() ? rejectionReason : "Seeded rejection",
                    rejectedTime);
        }

        return 1;
    }

    private void verifyAccountExistence() {
        Arrays.asList(
                "employee1@test.com", "employee2@test.com",
                "employee3@test.com", "employee4@test.com",
                "employee5@test.com", "employee6@test.com",
                "manager1@test.com", "manager2@test.com",
                "manager3@test.com", "finance1@test.com",
                "finance2@test.com", "auditor1@test.com",
                "admin@test.com").forEach(email -> {
                    boolean exists = userRepository.findByEmail(email).isPresent();
                    System.out.println("[DataSeeder] Account " + email + " : " + (exists ? "EXISTS" : "MISSING"));
                });
    }

    private void seedRoles() {
        for (ERole er : ERole.values()) {
            if (roleRepository.findByName(er).isEmpty()) {
                Role role = new Role();
                role.setName(er);
                roleRepository.save(role);
            }
        }
    }

    private void assignRole(User user, ERole er) {
        Role role = roleRepository.findByName(er).orElseThrow();
        user.getRoles().add(role);
        userRepository.save(user);
    }

    private Department ensureDepartment(String name, String description, String mealsLimit, String travelLimit,
            String accommodationLimit, String officeSuppliesLimit, String entertainmentLimit, String medicalLimit,
            String singleExpenseBlockLimit, String monthlyBudget) {
        return departmentRepository.findByName(name).orElseGet(() -> save(Department.builder()
                .name(name)
                .description(description)
                .mealsLimit(bd(mealsLimit))
                .travelLimit(bd(travelLimit))
                .accommodationLimit(bd(accommodationLimit))
                .officeSuppliesLimit(bd(officeSuppliesLimit))
                .entertainmentLimit(bd(entertainmentLimit))
                .medicalLimit(bd(medicalLimit))
                .singleExpenseBlockLimit(bd(singleExpenseBlockLimit))
                .monthlyBudget(bd(monthlyBudget))
                .isActive(true)
                .build()));
    }

    private User ensureUser(String email, String password, String name, String role, Department department,
            String designation, String employeeId, ERole eRole) {
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            return existing;
        }

        User user = save(buildUser(email, password, name, role, department, designation, employeeId));
        assignRole(user, eRole);
        return user;
    }

    private void ensureComplianceRule(String ruleName, String description, String evaluationJson, String action) {
        boolean exists = ruleRepository.findAll().stream()
                .anyMatch(rule -> ruleName.equals(rule.getRuleName()));
        if (!exists) {
            save(ComplianceRule.builder()
                    .ruleName(ruleName)
                    .description(description)
                    .evaluationJson(evaluationJson)
                    .action(action)
                    .isActive(true)
                    .build());
        }
    }

    private User buildUser(String email, String password, String name, String role, Department department,
            String designation, String employeeId) {
        return User.builder()
                .email(email)
                .username(email.split("@")[0])
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(role)
                .department(department)
                .designation(designation)
                .employeeId(employeeId)
                .active(true)
                .isActive(true)
                .roles(new java.util.HashSet<>())
                .build();
    }

    private Expense buildExpense(User user, String title, BigDecimal amount, String category, String status,
            String description, LocalDateTime createdAt, boolean isAnomaly, double anomalyScore, String riskLevel,
            int flagCount, String flagReasons, String deptName) {
        boolean flagged = isAnomaly || flagCount > 0;
        return Expense.builder()
                .user(user)
                .title(title)
                .amount(amount)
                .category(category)
                .status(status)
                .description(description)
                .createdAt(createdAt)
                .expenseDate(createdAt.toLocalDate())
                .department(user != null ? user.getDepartment() : null)
                .departmentName(deptName == null ? "" : deptName)
                .currency("INR")
                .flagged(flagged)
                .isAnomaly(isAnomaly)
                .anomalyScore(isAnomaly ? anomalyScore : null)
                .anomalyReason(isAnomaly && flagReasons != null && !flagReasons.isBlank() ? flagReasons : null)
                .flagCount(flagCount)
                .flagReasons(flagReasons == null || flagReasons.isBlank() ? null : flagReasons)
                .riskLevel(riskLevel == null || riskLevel.isBlank() ? "LOW" : riskLevel)
                .riskScore(calculateRiskScore(isAnomaly, flagCount, riskLevel))
                .violationDetails(flagReasons == null || flagReasons.isBlank() ? null : flagReasons)
                .build();
    }

    private int calculateRiskScore(boolean isAnomaly, int flagCount, String riskLevel) {
        int score = flagCount * 20;
        if (isAnomaly) {
            score += 35;
        }
        if ("MEDIUM".equalsIgnoreCase(riskLevel)) {
            score += 10;
        } else if ("HIGH".equalsIgnoreCase(riskLevel)) {
            score += 25;
        } else if ("CRITICAL".equalsIgnoreCase(riskLevel)) {
            score += 40;
        }
        return Math.min(score, 100);
    }

    private void createApprovalRecord(Expense expense, User approver, LocalDateTime approvalTime, String status,
            String comments, String rejectionReason) {
        if (approver == null) {
            return;
        }

        Approval approval = new Approval();
        approval.setExpense(expense);
        approval.setApprover(approver);
        approval.setStatus(status);
        approval.setComments(comments);
        approval.setTimestamp(approvalTime);
        approval.setRejectionReason(rejectionReason);
        approvalRepository.save(approval);

        if ("APPROVED".equalsIgnoreCase(status)) {
            expense.setApprovedAt(approvalTime);
            expense.setFirstApproverId(approver.getId());
            expenseRepository.save(expense);
        }
    }

    private void logExpense(Expense expense, String afterStatus, User actor, String actorRole, String summary,
            LocalDateTime time) {
        auditLogRepository.save(AuditLog.builder()
                .action("STATUS_CHANGE")
                .entityType("Expense")
                .entityId(expense.getId())
                .performedBy(actor != null ? actor.getEmail() : "SYSTEM")
                .performedByRole(actorRole)
                .afterState(afterStatus)
                .changeSummary(summary)
                .timestamp(time)
                .wasSystemTriggered(actor == null)
                .build());
    }

    @SuppressWarnings("unchecked")
    private <T> T save(T entity) {
        if (entity instanceof Department) {
            return (T) departmentRepository.save((Department) entity);
        }
        if (entity instanceof User) {
            return (T) userRepository.save((User) entity);
        }
        if (entity instanceof Expense) {
            return (T) expenseRepository.save((Expense) entity);
        }
        if (entity instanceof ComplianceRule) {
            return (T) ruleRepository.save((ComplianceRule) entity);
        }
        return entity;
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
