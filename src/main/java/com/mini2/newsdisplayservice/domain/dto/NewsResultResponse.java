package com.mini2.newsdisplayservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class NewsResultResponse {
    private List<NewsResponse> newsList;
    private Integer start;
}
