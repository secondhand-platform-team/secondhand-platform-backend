package com.secondhand.coreservice.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryAttributeRequest {

    @NotBlank(message = "Attribute code is required")
    private String code;

    @NotBlank(message = "Attribute name is required")
    private String name;

    private String description;

    @NotBlank(message = "Data type is required")
    private String dataType; // TEXT, NUMBER, SELECT, MULTI_SELECT, BOOLEAN, DATE

    private String unit;

    @Builder.Default
    private Boolean required = false;

    @Builder.Default
    private Boolean filterable = true;

    @Builder.Default
    private Boolean searchable = false;

    private BigDecimal minValueNumber;

    private BigDecimal maxValueNumber;

    private String optionsJson;

    @Builder.Default
    private Integer sortOrder = 0;
}
