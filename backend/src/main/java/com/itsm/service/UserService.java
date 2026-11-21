package com.itsm.service;

import com.itsm.dto.RegisterRequest;
import com.itsm.dto.UserUpdateRequest;
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

import com.itsm.dto.admin.TechnicianProfileRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** Public self-registration: always creates an EMPLOYEE, whatever the request says. */
    @Transactional
    public User registerEmployee(RegisterRequest request) {
        return create(request, UserRole.EMPLOYEE);
    }

    /** Admin user creation: the admin chooses the role. */
    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        return mapToResponse(create(request, request.getRole() != null ? request.getRole() : UserRole.EMPLOYEE));
    }

    private User create(RegisterRequest request, UserRole role) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already in use: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

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

        return savedUser;
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
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
        if (request.getRole() != null && request.getRole() != user.getRole()) {
            user.setRole(request.getRole());
            if (request.getRole() == UserRole.TECHNICIAN && technicianProfileRepository.findByUser(user).isEmpty()) {
                technicianProfileRepository.save(TechnicianProfile.builder()
                        .user(user).skills(new ArrayList<>(List.of("GENERAL")))
                        .specialization("General IT Support").maxActiveIncidents(10).available(true).build());
            }
        }

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void deactivateUser(Long id, String actingUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        if (user.getUsername().equals(actingUsername)) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        user.setActive(false);
        userRepository.save(user);
        technicianProfileRepository.findByUser(user).ifPresent(p -> {
            p.setAvailable(false);
            technicianProfileRepository.save(p);
        });
    }

    @Transactional
    public UserResponse activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(true);
        return mapToResponse(userRepository.save(user));
    }

    public List<Map<String, Object>> getTechnicianProfiles() {
        return technicianProfileRepository.findAll().stream().map(this::profileMap).toList();
    }

    public Map<String, Object> getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return technicianProfileRepository.findByUser(user).map(this::profileMap)
                .orElseThrow(() -> new ResourceNotFoundException("Technician profile", "user", username));
    }

    /** Creates the profile if missing; null fields in the request are left unchanged. */
    @Transactional
    public Map<String, Object> upsertTechnicianProfile(Long userId, TechnicianProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() != UserRole.TECHNICIAN && user.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Only technicians and admins can have a technician profile");
        }
        TechnicianProfile profile = technicianProfileRepository.findByUser(user)
                .orElse(TechnicianProfile.builder().user(user).skills(new ArrayList<>())
                        .certifications(new ArrayList<>()).build());
        if (request.skills() != null) {
            profile.setSkills(new ArrayList<>(request.skills().stream()
                    .map(sk -> sk.trim().toUpperCase()).distinct().toList()));
        }
        if (request.specialization() != null) {
            profile.setSpecialization(request.specialization().trim());
        }
        if (request.maxActiveIncidents() != null) {
            if (request.maxActiveIncidents() < profile.getCurrentActiveIncidents()) {
                throw new BadRequestException("Capacity cannot be lower than the " + profile.getCurrentActiveIncidents()
                        + " incidents this technician already has");
            }
            profile.setMaxActiveIncidents(request.maxActiveIncidents());
        }
        if (request.available() != null) {
            profile.setAvailable(request.available());
        }
        return profileMap(technicianProfileRepository.save(profile));
    }

    private Map<String, Object> profileMap(TechnicianProfile tp) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", tp.getUser().getId());
        map.put("fullName", tp.getUser().getFullName());
        map.put("username", tp.getUser().getUsername());
        map.put("department", tp.getUser().getDepartment() != null ? tp.getUser().getDepartment() : "IT Support");
        map.put("skills", tp.getSkills() != null ? tp.getSkills() : Collections.emptyList());
        map.put("specialization", tp.getSpecialization() != null ? tp.getSpecialization() : "");
        map.put("maxActiveIncidents", tp.getMaxActiveIncidents());
        map.put("currentActiveIncidents", tp.getCurrentActiveIncidents());
        map.put("totalResolvedIncidents", tp.getTotalResolvedIncidents());
        map.put("avgResolutionMinutes", Math.round(tp.getAvgResolutionTimeMinutes()));
        map.put("available", tp.isAvailable());
        map.put("active", tp.getUser().isActive());
        return map;
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
