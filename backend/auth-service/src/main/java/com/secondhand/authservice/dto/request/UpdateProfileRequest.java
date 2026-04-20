package com.secondhand.authservice.dto.request;

import com.secondhand.authservice.model.enums.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProfileRequest {
    private String fullName;
    private String phoneNumber;
    private String bio;
    private Gender gender;
    private LocalDate dateOfBirth;
}
