package com.secondhand.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserInfoResponse {
    private String userId;
    private String email;
    private String phoneNumber;
    private String role;
    private boolean status;
    private int freeSellUse;
    private LocalDate createdAt;
}
