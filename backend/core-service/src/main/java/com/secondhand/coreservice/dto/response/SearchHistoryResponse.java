package com.secondhand.coreservice.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchHistoryResponse {

    private String id;

    @JsonProperty("searchQuery")
    private String searchQuery;

    @JsonProperty("categoryId")
    private String categoryId;

    @JsonProperty("resultCount")
    private Integer resultCount;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
