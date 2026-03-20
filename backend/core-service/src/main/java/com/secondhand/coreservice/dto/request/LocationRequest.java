package com.secondhand.coreservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequest {
    private String address;
    private String ward;
    private String district;
    private String city;
}
