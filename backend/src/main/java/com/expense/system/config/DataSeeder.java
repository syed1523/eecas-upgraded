package com.expense.system.config;

import com.expense.system.entity.*;
import com.expense.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final DepartmentRepository departmentRepository;
        private final ExpenseRepository expenseRepository;
        private final ComplianceRuleRepository ruleRepository;
        private final AuditLogRepository auditLogRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                if (departmentRepository.count() > 0) {
                        System.out.println("--- DB ALREADY SEEDED, SKIPPING SEEDER ---");
                        java.util.List<User> users = userRepository.findAll();
                        users.forEach(u -> {
                                u.setPassword(passwordEncoder.encode("password"));
                                userRepository.save(u);
                        });
                        System.out.println("[DataSeeder] Passwords unconditionally reset.");
                        return;
                }
                System.out.println("--- AEGIS DATA RESET INITIATED ---");

                // Find existing admin to preserve
                Optional<User> adminOptional = userRepository.findByEmail("admin@test.com");

                auditLogRepository.deleteAll();
                expenseRepository.deleteAll();
                userRepository.deleteAll();
                departmentRepository.deleteAll();
                ruleRepository.deleteAll();

                // 2. SEED DEPARTMENTS
                Department eng = save(Department.builder().name("Engineering").description("Core Product Development")
                                .mealsLimit(bd("800")).travelLimit(bd("5000")).accommodationLimit(bd("4000"))
                                .officeSuppliesLimit(bd("2000")).entertainmentLimit(bd("1000")).medicalLimit(bd("5000"))
                                .singleExpenseBlockLimit(bd("10000")).monthlyBudget(bd("100000")).isActive(true)
                                .build());
                Department mkt = save(Department.builder().name("Marketing").description("Growth and Brand")
                                .mealsLimit(bd("1200")).travelLimit(bd("8000")).accommodationLimit(bd("6000"))
                                .officeSuppliesLimit(bd("1500")).entertainmentLimit(bd("5000")).medicalLimit(bd("5000"))
                                .singleExpenseBlockLimit(bd("15000")).monthlyBudget(bd("200000")).isActive(true)
                                .build());
                Department hr = save(Department.builder().name("HR").description("People and Culture")
                                .mealsLimit(bd("600")).travelLimit(bd("3000")).accommodationLimit(bd("3000"))
                                .officeSuppliesLimit(bd("1000")).entertainmentLimit(bd("2000")).medicalLimit(bd("5000"))
                                .singleExpenseBlockLimit(bd("5000")).monthlyBudget(bd("50000")).isActive(true).build());
                Department fin = save(Department.builder().name("Finance").description("Finance and Audit")
                                .mealsLimit(bd("500")).travelLimit(bd("2000")).accommodationLimit(bd("2000"))
                                .officeSuppliesLimit(bd("500")).entertainmentLimit(bd("500")).medicalLimit(bd("5000"))
                                .singleExpenseBlockLimit(bd("5000")).monthlyBudget(bd("30000")).isActive(true).build());

                // 3. SEED ROLES
                seedRoles();

                // 4. SEED USERS (14 Accounts)
                // Admin
                User admin = adminOptional.orElseGet(() -> buildUser("admin@test.com", "password", "Admin User",
                                "ADMIN", fin, "System Admin", "EMP-000"));
                admin.setRole("ADMIN");
                admin.setDepartment(fin);
                save(admin);
                assignRole(admin, ERole.ROLE_ADMIN);

                // Engineering
                User ravi = save(buildUser("employee1@test.com", "password", "Ravi Kumar", "EMPLOYEE", eng,
                                "Senior Dev", "EMP-101"));
                User arjun = save(buildUser("employee2@test.com", "password", "Arjun Singh", "EMPLOYEE", eng,
                                "QA Engineer", "EMP-102"));
                User sneha = save(buildUser("employee3@test.com", "password", "Sneha Nair", "EMPLOYEE", eng, "DevOps",
                                "EMP-103"));
                User sunita = save(buildUser("manager1@test.com", "password", "Sunita Patel", "MANAGER", eng,
                                "Engineering Manager", "EMP-M1"));

                assignRole(ravi, ERole.ROLE_EMPLOYEE);
                assignRole(arjun, ERole.ROLE_EMPLOYEE);
                assignRole(sneha, ERole.ROLE_EMPLOYEE);
                assignRole(sunita, ERole.ROLE_MANAGER);

                // Marketing
                User divya = save(buildUser("employee4@test.com", "password", "Divya Krishnan", "EMPLOYEE", mkt,
                                "Brand Manager", "EMP-201"));
                User karan = save(buildUser("employee5@test.com", "password", "Karan Mehta", "EMPLOYEE", mkt,
                                "SEO Specialist", "EMP-202"));
                User raj = save(buildUser("manager2@test.com", "password", "Raj Mehta", "MANAGER", mkt,
                                "Marketing Head", "EMP-M2"));

                assignRole(divya, ERole.ROLE_EMPLOYEE);
                assignRole(karan, ERole.ROLE_EMPLOYEE);
                assignRole(raj, ERole.ROLE_MANAGER);

                // HR
                User pooja = save(buildUser("employee6@test.com", "password", "Pooja Iyer", "EMPLOYEE", hr,
                                "HR Recruiter", "EMP-301"));
                User anita = save(buildUser("manager3@test.com", "password", "Anita Sharma", "MANAGER", hr,
                                "HR Director", "EMP-M3"));

                assignRole(pooja, ERole.ROLE_EMPLOYEE);
                assignRole(anita, ERole.ROLE_MANAGER);

                // Finance / Audit
                User deepak = save(buildUser("finance1@test.com", "password", "Deepak Menon", "FINANCE", fin,
                                "Finance Analyst", "EMP-F1"));
                User priya = save(buildUser("finance2@test.com", "password", "Priya Sharma", "FINANCE", fin,
                                "Tax Consultant", "EMP-F2"));
                User kavitha = save(buildUser("auditor1@test.com", "password", "Kavitha Rao", "AUDITOR", fin,
                                "Risk Auditor", "EMP-A1"));

                assignRole(deepak, ERole.ROLE_FINANCE);
                assignRole(priya, ERole.ROLE_FINANCE);
                assignRole(kavitha, ERole.ROLE_AUDITOR);

                // 5. SEED COMPLIANCE RULES
                save(ComplianceRule.builder().ruleName("Daily Food Limit").description("Max 800 per day")
                                .evaluationJson("{\"type\":\"limit\",\"category\":\"MEALS\",\"max\":800}")
                                .action("FLAG").isActive(true).build());
                save(ComplianceRule.builder().ruleName("Weekend Submission").description("Flag weekend submissions")
                                .evaluationJson("{\"type\":\"weekend\"}").action("FLAG").isActive(true).build());
                save(ComplianceRule.builder().ruleName("Alcohol Policy").description("No entertainment above 5000")
                                .evaluationJson("{\"type\":\"limit\",\"category\":\"ENTERTAINMENT\",\"max\":5000}")
                                .action("BLOCK").isActive(true).build());
                save(ComplianceRule.builder().ruleName("Distance Check").description("Travel must be above 50km")
                                .evaluationJson("{\"type\":\"custom\",\"logic\":\"travelDist > 50\"}").action("WARN")
                                .isActive(true).build());
                save(ComplianceRule.builder().ruleName("Duplicate Receipt").description("Same hash check")
                                .evaluationJson("{\"type\":\"duplicate\"}").action("BLOCK").isActive(true).build());
                save(ComplianceRule.builder().ruleName("Late Night Travel").description("Travel after 10PM")
                                .evaluationJson("{\"type\":\"time\",\"after\":\"22:00\"}").action("FLAG").isActive(true)
                                .build());
                save(ComplianceRule.builder().ruleName("Medical Limit").description("Annual medical cap")
                                .evaluationJson("{\"type\":\"limit\",\"category\":\"MEDICAL\",\"max\":15000}")
                                .action("FLAG").isActive(true).build());

                // 6. SEED EXPENSES (17 Scenarios)

                // Scenario 1: Clean flow (ravi -> ravini manager Sunita)
                Expense s1e1 = save(buildExpense(ravi, "AWS Certification", bd("12000"), "OFFICE_SUPPLIES", "APPROVED",
                                "Cloud training", LocalDateTime.now().minusDays(10), false, 0, "LOW", 0, "",
                                "Engineering"));
                logExpense(s1e1, "SUBMITTED", ravi, "USER", "Initial submission",
                                LocalDateTime.now().minusDays(10).plusHours(1));
                logExpense(s1e1, "APPROVED", sunita, "MANAGER", "Manager Approval", LocalDateTime.now().minusDays(9));
                logExpense(s1e1, "PAID", deepak, "FINANCE", "Payment processed", LocalDateTime.now().minusDays(8));

                // Scenario 2: Rejected (ravi)
                Expense s2e1 = save(buildExpense(ravi, "Team Pizza", bd("5500"), "MEALS", "REJECTED", "Monthly hangout",
                                LocalDateTime.now().minusDays(15), true, 2.5, "MEDIUM", 1, "Exceeds daily meal limit",
                                "Engineering"));
                logExpense(s2e1, "SUBMITTED", ravi, "USER", "Submission", LocalDateTime.now().minusDays(15));
                logExpense(s2e1, "REJECTED", sunita, "MANAGER", "Exceeds department budget for snacks",
                                LocalDateTime.now().minusDays(14));

                // Scenario 3: Anomaly Detected (divya)
                Expense s3e1 = save(buildExpense(divya, "Client Lunch", bd("8500"), "ENTERTAINMENT", "UNDER_REVIEW",
                                "High-end restaurant", LocalDateTime.now().minusDays(2), true, 4.2, "CRITICAL", 3,
                                "Statistical Anomaly; Outlier Amount; Pattern Flag", "Marketing"));
                logExpense(s3e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(2));
                logExpense(s3e1, "FLAGGED", null, "SYSTEM", "Statistical Anomaly Detected (Z-score 4.2)",
                                LocalDateTime.now().minusDays(2).plusMinutes(1));

                // Scenario 4: Pending Approvals (ravi)
                Expense s4e1 = save(buildExpense(ravi, "Internet Bill", bd("1500"), "UTILITIES", "PENDING",
                                "Monthly reimbursement", LocalDateTime.now().minusHours(12), false, 0, "LOW", 0, "",
                                "Engineering"));
                logExpense(s4e1, "SUBMITTED", ravi, "USER", "Submission", LocalDateTime.now().minusHours(12));

                // Scenario 5: Draft (sneha)
                Expense s5e1 = save(buildExpense(sneha, "Monitor Stand", bd("2500"), "OFFICE_SUPPLIES", "DRAFT",
                                "Ergonomics", LocalDateTime.now().minusHours(24), false, 0, "LOW", 0, "",
                                "Engineering"));

                // Scenario 6: Multiple Pattern Flag (karan)
                Expense s6e1 = save(buildExpense(karan, "Business Trip Uber", bd("450"), "TRAVEL", "PENDING",
                                "Airport drop", LocalDateTime.now().minusDays(1), false, 0, "MEDIUM", 2,
                                "Duplicate merchant pattern; Frequent travel", "Marketing"));
                logExpense(s6e1, "SUBMITTED", karan, "USER", "Submission", LocalDateTime.now().minusDays(1));

                // Scenario 7: Weekend Submission (divya)
                Expense s7e1 = save(buildExpense(divya, "Booth Rental", bd("25000"), "TRAVEL", "PENDING",
                                "Event booking", LocalDateTime.now().minusDays(1), false, 1.2, "MEDIUM", 1,
                                "Weekend Submission", "Marketing"));
                logExpense(s7e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(1));

                // Scenario 8: Manager's own expense (sunita)
                Expense s8e1 = save(buildExpense(sunita, "Offsite Venue", bd("45000"), "TRAVEL", "PENDING",
                                "Team retreat", LocalDateTime.now(), false, 0, "LOW", 0, "", "Engineering"));
                logExpense(s8e1, "SUBMITTED", sunita, "MANAGER", "Submission", LocalDateTime.now());

                // Scenario 9: Auditor Cleared (arjun)
                Expense s9e1 = save(buildExpense(arjun, "Medical Checkup", bd("2200"), "MEDICAL", "APPROVED",
                                "Annual check", LocalDateTime.now().minusDays(20), true, 0, "LOW", 1,
                                "Statistical Anomaly", "Engineering"));
                logExpense(s9e1, "SUBMITTED", arjun, "USER", "Submission", LocalDateTime.now().minusDays(20));
                logExpense(s9e1, "AUDIT_REVIEW", kavitha, "AUDITOR", "Manual verification pending",
                                LocalDateTime.now().minusDays(19));
                logExpense(s9e1, "CLEARED", kavitha, "AUDITOR", "Valid medical claim after review",
                                LocalDateTime.now().minusDays(18));
                logExpense(s9e1, "APPROVED", sunita, "MANAGER", "Manager Approval", LocalDateTime.now().minusDays(17));

                // Scenario 10: Finance Review (pooja)
                Expense s10e1 = save(buildExpense(pooja, "Job Portal Ad", bd("12000"), "OFFICE_SUPPLIES", "PENDING",
                                "LinkedIn hiring", LocalDateTime.now().minusDays(3), false, 0, "LOW", 0, "", "HR"));
                logExpense(s10e1, "SUBMITTED", pooja, "USER", "Submission", LocalDateTime.now().minusDays(3));

                // Scenario 11: Accommodation (divya)
                Expense s11e1 = save(buildExpense(divya, "Hotel Stay", bd("12500"), "ACCOMMODATION", "PENDING",
                                "Conference", LocalDateTime.now().minusDays(5), false, 0, "LOW", 0, "", "Marketing"));
                logExpense(s11e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(5));

                // Scenario 12: High Risk Blocked (karan)
                Expense s12e1 = save(buildExpense(karan, "Alcohol and Party", bd("50000"), "ENTERTAINMENT", "REJECTED",
                                "Team party", LocalDateTime.now().minusDays(40), true, 6.5, "CRITICAL", 5,
                                "Policy Violation: Alcohol; High Amount; Pattern Suspect", "Marketing"));
                logExpense(s12e1, "SUBMITTED", karan, "USER", "Submission", LocalDateTime.now().minusDays(40));
                logExpense(s12e1, "REJECTED", raj, "MANAGER", "Policy violation", LocalDateTime.now().minusDays(39));

                // Scenario 13: Large Travel (sunita)
                Expense s13e1 = save(buildExpense(sunita, "Flight to London", bd("65000"), "TRAVEL", "PENDING",
                                "Quarterly planning", LocalDateTime.now().minusHours(24), false, 0, "LOW", 0, "",
                                "Engineering"));
                logExpense(s13e1, "SUBMITTED", sunita, "MANAGER", "Submission", LocalDateTime.now().minusHours(24));

                // Scenario 14: HR Training (Anita)
                Expense s14e1 = save(buildExpense(anita, "Leadership Workshop", bd("35000"), "OFFICE_SUPPLIES",
                                "APPROVED", "External training", LocalDateTime.now().minusDays(60), false, 0, "LOW", 0,
                                "", "HR"));
                logExpense(s14e1, "SUBMITTED", anita, "MANAGER", "Submission", LocalDateTime.now().minusDays(60));
                logExpense(s14e1, "APPROVED", admin, "ADMIN", "Admin auto-approval", LocalDateTime.now().minusDays(59));

                // Scenario 15: Small Supplies (divya)
                Expense s15e1 = save(buildExpense(divya, "Notebooks", bd("250"), "OFFICE_SUPPLIES", "PAID",
                                "Stationary", LocalDateTime.now().minusDays(30), false, 0, "LOW", 0, "", "Marketing"));
                logExpense(s15e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(30));
                logExpense(s15e1, "APPROVED", raj, "MANAGER", "Manager Approval", LocalDateTime.now().minusDays(29));
                logExpense(s15e1, "PAID", priya, "FINANCE", "Payment processed", LocalDateTime.now().minusDays(28));

                // Scenario 16: Anomaly Cleared Manual (divya)
                Expense s16e1 = save(buildExpense(divya, "Urgent Courier", bd("1200"), "TRAVEL", "APPROVED",
                                "Client docs", LocalDateTime.now().minusDays(4), true, 2.8, "MEDIUM", 1,
                                "Amount outlier", "Marketing"));
                logExpense(s16e1, "SUBMITTED", divya, "USER", "Submission", LocalDateTime.now().minusDays(4));
                logExpense(s16e1, "AUDIT_REVIEW", kavitha, "AUDITOR", "Verifying receipt",
                                LocalDateTime.now().minusDays(3));
                logExpense(s16e1, "CLEARED", kavitha, "AUDITOR", "Approved after review",
                                LocalDateTime.now().minusDays(2));

                // Scenario 17: Medical (pooja)
                Expense s17e1 = save(buildExpense(pooja, "Pharma Bill", bd("500"), "MEDICAL", "PENDING", "Medicine",
                                LocalDateTime.now().minusHours(1), false, 0, "LOW", 0, "", "HR"));
                logExpense(s17e1, "SUBMITTED", pooja, "USER", "Submission", LocalDateTime.now().minusHours(1));

                System.out.println("--- AEGIS DATA SEEDING COMPLETE ---");
                System.out.println("Accounts: 14 | Scenarios: 17 | Departments: 4");

                verifyPasswords();
                verifyAccountExistence();
        }

        private void verifyPasswords() {
                System.out.println("[DataSeeder] Starting password verification...");
                java.util.List<User> users = userRepository.findAll();
                users.forEach(user -> {
                                user.setPassword(passwordEncoder.encode("password"));
                                userRepository.save(user);
                                System.out.println("[DataSeeder] Re-encoded password for: " + user.getEmail());
                });
                System.out.println("[DataSeeder] Password verification complete");
        }

        private void verifyAccountExistence() {
                java.util.List<String> expectedEmails = java.util.Arrays.asList(
                                "employee1@test.com", "employee2@test.com",
                                "employee3@test.com", "employee4@test.com",
                                "employee5@test.com", "employee6@test.com",
                                "manager1@test.com", "manager2@test.com",
                                "manager3@test.com", "finance1@test.com",
                                "finance2@test.com", "auditor1@test.com",
                                "admin@test.com");

                expectedEmails.forEach(email -> {
                        boolean exists = userRepository.findByEmail(email).isPresent();
                        System.out.println(
                                        "[DataSeeder] Account " + email + " : " + (exists ? "EXISTS ✓" : "MISSING ✗"));
                });
        }

        private void seedRoles() {
                for (ERole er : ERole.values()) {
                        if (!roleRepository.findByName(er).isPresent()) {
                                Role r = new Role();
                                r.setName(er);
                                roleRepository.save(r);
                        }
                }
        }

        private void assignRole(User user, ERole er) {
                Role r = roleRepository.findByName(er).orElseThrow();
                user.getRoles().add(r);
                userRepository.save(user);
        }

        private User buildUser(String email, String password, String name, String role, Department dept,
                        String designation, String employeeId) {
                return User.builder()
                                .email(email)
                                .username(email.split("@")[0])
                                .password(passwordEncoder.encode(password))
                                .name(name)
                                .role(role)
                                .department(dept)
                                .designation(designation)
                                .employeeId(employeeId)
                                .active(true)
                                .isActive(true)
                                .roles(new java.util.HashSet<>())
                                .build();
        }

        private Expense buildExpense(User user, String title, BigDecimal amount, String category, String status,
                        String description, LocalDateTime createdAt, boolean isAnomaly, double anomalyScore,
                        String riskLevel, int flagCount, String flagReasons, String deptName) {
                return Expense.builder()
                                .user(user)
                                .title(title)
                                .amount(amount)
                                .category(category)
                                .status(status)
                                .description(description)
                                .createdAt(createdAt)
                                .isAnomaly(isAnomaly)
                                .anomalyScore(isAnomaly ? anomalyScore : null)
                                .riskLevel(riskLevel)
                                .flagCount(flagCount)
                                .flagReasons(flagReasons.isEmpty() ? null : flagReasons)
                                .departmentName(deptName)
                                .currency("INR")
                                .riskScore(flagCount * 20)
                                .build();
        }

        private void logExpense(Expense exp, String afterStatus, User actor, String actorRole, String summary,
                        LocalDateTime time) {
                auditLogRepository.save(AuditLog.builder()
                                .action("STATUS_CHANGE")
                                .entityType("Expense")
                                .entityId(exp.getId())
                                .performedBy(actor != null ? actor.getEmail() : "SYSTEM")
                                .performedByRole(actorRole)
                                .afterState(afterStatus)
                                .changeSummary(summary)
                                .timestamp(time)
                                .wasSystemTriggered(actor == null)
                                .build());
        }

        private <T> T save(T entity) {
                if (entity instanceof Department)
                        return (T) departmentRepository.save((Department) entity);
                if (entity instanceof User)
                        return (T) userRepository.save((User) entity);
                if (entity instanceof Expense)
                        return (T) expenseRepository.save((Expense) entity);
                if (entity instanceof ComplianceRule)
                        return (T) ruleRepository.save((ComplianceRule) entity);
                return entity;
        }

        private BigDecimal bd(String val) {
                return new BigDecimal(val);
        }
}
