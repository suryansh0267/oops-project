package com.itsm.controller;

import com.itsm.dto.AuthResponse;
import com.itsm.dto.LoginRequest;
import com.itsm.dto.RegisterRequest;
import com.itsm.entity.User;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.UserRepository;
import com.itsm.security.JwtTokenProvider;
import com.itsm.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private final UserService userService;

    /** Wrong password -> 401 "Incorrect username or password"; deactivated -> 401 with its own message. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));
        return toResponse(jwtTokenProvider.generateToken(auth), user);
    }

    /**
     * Self-registration always creates an EMPLOYEE account. Technicians and admins
     * are created by an administrator, so nobody can grant themselves privileges.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerEmployee(request);
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(jwtTokenProvider.generateToken(auth), user));
    }

    private static AuthResponse toResponse(String jwt, User user) {
        return AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .tokenType("Bearer")
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
