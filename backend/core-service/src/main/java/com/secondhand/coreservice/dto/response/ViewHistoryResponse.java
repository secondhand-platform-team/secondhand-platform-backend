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
public class ViewHistoryResponse {

    private String id;

    @JsonProperty("itemId")
    private String itemId;

    @JsonProperty("viewedAt")
    private LocalDateTime viewedAt;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("item")
    private ItemResponse item;

    @JsonProperty("viewCount")
    private Long viewCount;
}
