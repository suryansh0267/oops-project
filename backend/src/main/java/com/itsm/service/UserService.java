package com.itsm.service;

import com.itsm.dto.RegisterRequest;
import com.itsm.dto.UserResponse;
import com.itsm.entity.TechnicianProfile;
import com.itsm.entity.User;
import com.itsm.enums.UserRole;
import com.itsm.exception.BadRequestException;
import com.itsm.exception.DuplicateResourceException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.TechnicianProfileRepository;
import com.itsm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToResponse(user);
    }

    public List<UserResponse> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already in use: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.EMPLOYEE;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .department(request.getDepartment() != null ? request.getDepartment() : (role == UserRole.TECHNICIAN ? "IT Support" : "Operations"))
                .phoneNumber(request.getPhoneNumber())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        // If the new user is a Technician, provision an associated TechnicianProfile
        if (role == UserRole.TECHNICIAN) {
            List<String> defaultSkills = new ArrayList<>();
            if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
                defaultSkills.add(request.getDepartment().trim().toUpperCase());
            } else {
                defaultSkills.add("APPLICATION");
                defaultSkills.add("GENERAL");
            }

            TechnicianProfile profile = TechnicianProfile.builder()
                    .user(savedUser)
                    .skills(defaultSkills)
                    .specialization("General IT Support & Diagnostics")
                    .maxActiveIncidents(10)
                    .currentActiveIncidents(0)
                    .totalResolvedIncidents(0)
                    .available(true)
                    .build();

            technicianProfileRepository.save(profile);
        }

        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setPhoneNumber(request.getPhoneNumber());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public TechnicianProfile createTechnicianProfile(Long userId, List<String> skills, String specialization) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() != UserRole.TECHNICIAN && user.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("User must have TECHNICIAN or ADMIN role to have a technician profile");
        }

        TechnicianProfile profile = technicianProfileRepository.findByUser(user)
                .orElse(TechnicianProfile.builder()
                        .user(user)
                        .skills(new ArrayList<>())
                        .certifications(new ArrayList<>())
                        .build());

        if (skills != null) {
            profile.setSkills(new ArrayList<>(skills));
        }
        if (specialization != null) {
            profile.setSpecialization(specialization);
        }

        return technicianProfileRepository.save(profile);
    }

    public List<java.util.Map<String, Object>> getTechnicianProfiles() {
        return technicianProfileRepository.findAll().stream().map(tp -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("userId", tp.getUser().getId());
            map.put("fullName", tp.getUser().getFullName());
            map.put("username", tp.getUser().getUsername());
            map.put("department", tp.getUser().getDepartment() != null ? tp.getUser().getDepartment() : "IT Support");
            map.put("skills", tp.getSkills() != null ? tp.getSkills() : java.util.Collections.emptyList());
            map.put("specialization", tp.getSpecialization() != null ? tp.getSpecialization() : "");
            map.put("maxActiveIncidents", tp.getMaxActiveIncidents());
            map.put("currentActiveIncidents", tp.getCurrentActiveIncidents());
            map.put("totalResolvedIncidents", tp.getTotalResolvedIncidents());
            map.put("available", tp.isAvailable());
            return map;
        }).toList();
    }

    @Transactional
    public TechnicianProfile updateTechnicianProfile(Long userId, List<String> skills, Integer maxActive, String specialization, Boolean available) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        TechnicianProfile profile = technicianProfileRepository.findByUser(user)
                .orElse(TechnicianProfile.builder()
                        .user(user)
                        .skills(new ArrayList<>())
                        .certifications(new ArrayList<>())
                        .build());

        if (skills != null) {
            profile.setSkills(new ArrayList<>(skills));
        }
        if (specialization != null) {
            profile.setSpecialization(specialization);
        }
        if (maxActive != null) {
            profile.setMaxActiveIncidents(maxActive);
        }
        if (available != null) {
            profile.setAvailable(available);
        }

        return technicianProfileRepository.save(profile);
    }

    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .department(user.getDepartment())
                .phoneNumber(user.getPhoneNumber())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
