package com.expense.system.repository;

import com.expense.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByDepartmentId(Long departmentId);

    List<User> findByDepartmentIdAndRole(Long departmentId, String role);

    List<User> findByRole(String role);
}
