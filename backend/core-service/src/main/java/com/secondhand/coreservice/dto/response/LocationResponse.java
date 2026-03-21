package com.secondhand.coreservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponse {
    private String address;
    private String ward;
    private String district;
    private String city;
}
