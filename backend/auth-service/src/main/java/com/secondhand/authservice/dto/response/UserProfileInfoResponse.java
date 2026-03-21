package com.secondhand.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileInfoResponse {
    private UserInfoResponse user;

    @JsonProperty("user_profile")
    private UserProfileResponse userProfile;
}
