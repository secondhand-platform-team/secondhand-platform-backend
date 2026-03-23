package com.secondhand.coreservice.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    private String categoryId;
    private String name;
    private String description;
    private String parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
