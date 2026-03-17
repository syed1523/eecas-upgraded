package com.expense.system.repository;

import com.expense.system.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository
        extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);

    List<Department> findByIsActiveTrue();
}
