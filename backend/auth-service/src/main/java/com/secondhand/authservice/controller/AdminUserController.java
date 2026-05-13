package com.secondhand.authservice.controller;

import com.secondhand.authservice.dto.response.AdminUserResponse;
import com.secondhand.authservice.dto.response.MessageResponse;
import com.secondhand.authservice.model.User;
import com.secondhand.authservice.model.enums.Role;
import com.secondhand.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try {
                roleEnum = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role passed: {}", role);
            }
        }

        Page<User> users = userRepository.searchUsers(q, roleEnum, pageable);
        Page<AdminUserResponse> response = users.map(this::mapToAdminUserResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
        return ResponseEntity.ok(mapToAdminUserResponse(user));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable String userId,
            @RequestParam boolean status) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        user.setStatus(status);
        userRepository.save(user);

        log.info("Cập nhật trạng thái người dùng {} thành {}", userId, status ? "HOẠT ĐỘNG" : "BỊ KHÓA");
        return ResponseEntity.ok(mapToAdminUserResponse(user));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        List<User> allUsers = userRepository.findAll();

        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::isStatus).count();
        long lockedUsers = totalUsers - activeUsers;

        long staffCount = allUsers.stream().filter(u -> u.getRole() == Role.STAFF).count();
        long adminCount = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long standardUsers = totalUsers - staffCount - adminCount;

        // Group registrations by creation date (last 7 days)
        Map<LocalDate, Long> registrationStats = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(LocalDate.now().minusDays(7)))
                .collect(Collectors.groupingBy(User::getCreatedAt, Collectors.counting()));

        // Ensure days with 0 registrations are populated
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            registrationStats.putIfAbsent(date, 0L);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("lockedUsers", lockedUsers);
        stats.put("staffCount", staffCount);
        stats.put("adminCount", adminCount);
        stats.put("standardUsers", standardUsers);
        stats.put("registrationHistory", registrationStats);

        return ResponseEntity.ok(stats);
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        AdminUserResponse.AdminUserResponseBuilder builder = AdminUserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.isStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        if (user.getUserProfile() != null) {
            builder.fullName(user.getUserProfile().getFullName())
                   .avatarUrl(user.getUserProfile().getAvatarUrl())
                   .gender(user.getUserProfile().getGender() != null ? user.getUserProfile().getGender().toString() : null)
                   .bio(user.getUserProfile().getBio());
        }

        return builder.build();
    }
}
