package com.expense.system.config;

import com.expense.system.entity.ERole;
import com.expense.system.entity.Role;
import com.expense.system.entity.User;
import com.expense.system.repository.RoleRepository;
import com.expense.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        System.out.println("Seeding Roles...");
        for (ERole role : ERole.values()) {
            if (roleRepository.findByName(role).isEmpty()) {
                Role r = new Role();
                r.setName(role);
                roleRepository.saveAndFlush(r);
                System.out.println("Created Role: " + role);
            }
        }
        System.out.println("Roles Seeded.");

        // Seed Users
        /*
         * try {
         * System.out.println("Seeding Users...");
         * createUserIfNotFound("admin", "admin@test.com", ERole.ROLE_ADMIN);
         * createUserIfNotFound("manager", "manager@test.com", ERole.ROLE_MANAGER);
         * createUserIfNotFound("finance", "finance@test.com", ERole.ROLE_FINANCE);
         * createUserIfNotFound("auditor", "auditor@test.com", ERole.ROLE_AUDITOR);
         * createUserIfNotFound("employee", "employee@test.com", ERole.ROLE_EMPLOYEE);
         * System.out.println("Users Seeded.");
         * } catch (Exception e) {
         * System.err.println("Error seeding users: " + e.getMessage());
         * e.printStackTrace();
         * }
         */
    }

    private void createUserIfNotFound(String username, String email, ERole roleEnum) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("password"));

            Set<Role> roles = new HashSet<>();
            Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(role);
            user.setRoles(roles);

            userRepository.save(user);
            System.out.println("Seeded user: " + username);
        }
    }
}
