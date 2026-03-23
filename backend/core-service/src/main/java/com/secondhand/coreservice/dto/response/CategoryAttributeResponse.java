package com.secondhand.coreservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho category attributes
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryAttributeResponse {

    private String attributeId;

    private String code;

    private String name;

    private String description;

    private String dataType;

    private String unit;

    private Boolean required;

    private Boolean filterable;

    private Boolean searchable;

    private BigDecimal minValueNumber;

    private BigDecimal maxValueNumber;

    private String optionsJson;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
