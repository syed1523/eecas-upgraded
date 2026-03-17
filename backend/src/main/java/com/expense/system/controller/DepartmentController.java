package com.expense.system.controller;

import com.expense.system.entity.Department;
import com.expense.system.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" }, maxAge = 3600)
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @GetMapping
    public ResponseEntity<List<Department>> getAllActiveDepartments() {
        return ResponseEntity.ok(departmentRepository.findByIsActiveTrue());
    }
}
