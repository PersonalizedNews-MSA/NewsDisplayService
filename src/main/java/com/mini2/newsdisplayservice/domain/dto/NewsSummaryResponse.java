package com.mini2.newsdisplayservice.domain.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsSummaryResponse {
    private String summary;
}
