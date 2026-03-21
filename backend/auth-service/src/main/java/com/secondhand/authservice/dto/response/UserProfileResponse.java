package com.secondhand.authservice.dto.response;

import com.secondhand.authservice.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private String fullName;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String bio;
}
