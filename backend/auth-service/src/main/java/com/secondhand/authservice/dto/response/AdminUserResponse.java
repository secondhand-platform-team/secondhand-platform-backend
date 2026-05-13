package com.secondhand.authservice.dto.response;

import com.secondhand.authservice.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private String userId;
    private String email;
    private String phoneNumber;
    private Role role;
    private boolean status;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String fullName;
    private String avatarUrl;
    private String gender;
    private String bio;
}
