package com.expense.system.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private boolean active;
    private Boolean isActive;
    private Long departmentId;
    private String departmentName;
}
