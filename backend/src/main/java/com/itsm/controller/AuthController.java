package com.itsm.controller;

import com.itsm.dto.AuthResponse;
import com.itsm.dto.LoginRequest;
import com.itsm.dto.RegisterRequest;
import com.itsm.entity.User;
import com.itsm.enums.UserRole;
import com.itsm.repository.UserRepository;
import com.itsm.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final com.itsm.repository.TechnicianProfileRepository technicianProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        AuthResponse response = AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        UserRole role = registerRequest.getRole() != null ? registerRequest.getRole() : UserRole.EMPLOYEE;

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .role(role)
                .department(registerRequest.getDepartment() != null ? registerRequest.getDepartment() : (role == UserRole.TECHNICIAN ? "IT Support" : "Operations"))
                .phoneNumber(registerRequest.getPhoneNumber())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        if (role == UserRole.TECHNICIAN) {
            java.util.List<String> defaultSkills = new java.util.ArrayList<>();
            if (registerRequest.getDepartment() != null && !registerRequest.getDepartment().isBlank()) {
                defaultSkills.add(registerRequest.getDepartment().trim().toUpperCase());
            } else {
                defaultSkills.add("APPLICATION");
                defaultSkills.add("GENERAL");
            }

            com.itsm.entity.TechnicianProfile profile = com.itsm.entity.TechnicianProfile.builder()
                    .user(savedUser)
                    .skills(defaultSkills)
                    .specialization("General IT Support & Triage")
                    .maxActiveIncidents(10)
                    .currentActiveIncidents(0)
                    .totalResolvedIncidents(0)
                    .available(true)
                    .build();

            technicianProfileRepository.save(profile);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getUsername(),
                        registerRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateToken(authentication);

        AuthResponse response = AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
