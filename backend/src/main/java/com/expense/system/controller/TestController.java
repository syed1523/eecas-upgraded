package com.expense.system.controller;

import com.expense.system.entity.ERole;
import com.expense.system.entity.Role;
import com.expense.system.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public String seedRoles() {
        for (ERole role : ERole.values()) {
            if (roleRepository.findByName(role).isEmpty()) {
                Role r = new Role();
                r.setName(role);
                roleRepository.save(r);
            }
        }
        return "Roles Seeded Successfully";
    }
}
