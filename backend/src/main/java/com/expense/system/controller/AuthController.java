package com.expense.system.controller;

import com.expense.system.entity.Department;
import com.expense.system.entity.ERole;
import com.expense.system.entity.Role;
import com.expense.system.entity.User;
import com.expense.system.payload.request.LoginRequest;
import com.expense.system.payload.request.SignupRequest;
import com.expense.system.payload.response.JwtResponse;
import com.expense.system.payload.response.MessageResponse;
import com.expense.system.repository.DepartmentRepository;
import com.expense.system.repository.RoleRepository;
import com.expense.system.repository.UserRepository;
import com.expense.system.security.jwt.JwtUtils;
import com.expense.system.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles));
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Invalid email or password"));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Account is disabled"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of(
                            "error", e.getMessage() != null ? e.getMessage() : "Authentication failed",
                            "cause", e.getCause() != null ? e.getCause().getMessage() : "unknown"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_EMPLOYEE)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    // NOTE: "admin" case intentionally ABSENT.
                    // ROLE_ADMIN cannot be self-assigned via public signup.
                    // Admin accounts must be seeded via admin_bootstrap.sql or
                    // created by an existing admin through the Admin dashboard.
                    case "manager":
                        Role modRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                        break;
                    case "finance":
                        Role finRole = roleRepository.findByName(ERole.ROLE_FINANCE)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(finRole);
                        break;
                    case "compliance":
                        Role compRole = roleRepository.findByName(ERole.ROLE_COMPLIANCE)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(compRole);
                        break;
                    case "auditor":
                        Role audRole = roleRepository.findByName(ERole.ROLE_AUDITOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(audRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_EMPLOYEE)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        if (signUpRequest.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(signUpRequest.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Error: Department not found."));
            user.setDepartment(dept);
        } else {
            // Need to check if user is Auditor or Admin, they don't strictly require a
            // department, but others do.
            boolean isAuditorOrAdmin = roles.stream()
                    .anyMatch(r -> r.getName() == ERole.ROLE_AUDITOR || r.getName() == ERole.ROLE_ADMIN);
            if (!isAuditorOrAdmin) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Department selection is mandatory!"));
            }
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
