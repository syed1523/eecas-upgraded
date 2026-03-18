package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Entity
@Table(name = "users", uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String username;
        private String name;
        private String email;

        @Column(nullable = false)
        @JsonIgnore
        private String password;

        @Builder.Default
        private boolean active = true;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "department_id", nullable = true)
        @JsonIgnoreProperties({
                        "hibernateLazyInitializer", "handler", "users", "expenses"
        })
        private Department department;

        @Column(name = "employee_id", unique = true, nullable = true)
        private String employeeId;

        @Column(name = "designation", length = 100, nullable = true)
        private String designation;

        @Column(name = "is_active", nullable = true)
        @Builder.Default
        private Boolean isActive = true;

        private String role;

        // GDPR soft-delete
        private LocalDateTime deletedAt;
        private String deletedBy;

        @ManyToMany(fetch = FetchType.EAGER)
        @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
        @Builder.Default
        private Set<Role> roles = new HashSet<>();

        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JsonIgnore
        @Builder.Default
        private List<Expense> expenses = new ArrayList<>();
}
