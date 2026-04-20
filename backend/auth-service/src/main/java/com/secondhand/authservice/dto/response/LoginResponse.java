package com.secondhand.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Login response - tokens are delivered via HttpOnly cookies only.
 * The response body contains only user information.
 */
@Getter
@AllArgsConstructor
public class LoginResponse {
    private UserInfoResponse user;

    @JsonProperty("user_profile")
    private UserProfileResponse userProfile;
}
