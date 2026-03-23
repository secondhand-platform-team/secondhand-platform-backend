package com.secondhand.coreservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho giá trị thuộc tính của item
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemAttributeResponse {

    private String attributeId;

    private String code;

    private String name;

    private String description;

    private String dataType;

    private String unit;

  
    private Object value;
}
