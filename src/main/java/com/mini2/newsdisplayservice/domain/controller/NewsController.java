package com.mini2.newsdisplayservice.domain.controller;


import com.mini2.newsdisplayservice.domain.dto.NewsResponse;
import com.mini2.newsdisplayservice.domain.dto.NewsResultResponse;
import com.mini2.newsdisplayservice.domain.service.NewsService;
import com.mini2.newsdisplayservice.event.consumer.message.favorite.service.FavoriteNewsInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "News API", description = "뉴스 조회 및 AI 요약 관련 API")
@RequestMapping(value = "api/news/v1")
public class NewsController {

    private final NewsService newsService;


    //TODO 1L 로 입력한 것은 임시입니다. 지워야합니다.
    @Operation(summary = "헤드라인 뉴스 목록 조회", description = "최신 헤드라인 뉴스 목록을 조회합니다.")
    @GetMapping("/")
    public ResponseEntity<List<NewsResponse>> getNews() {
        List<NewsResponse> newsList = newsService.getNews(1L);
        return ResponseEntity.ok(newsList);
    }

    @Operation(summary = "AI 추천 뉴스 조회", description = "사용자의 관심사 기반 AI 추천 뉴스를 조회합니다.")
    @GetMapping({"/ai"})
    public ResponseEntity<NewsResultResponse> getPrompt(
            @RequestParam(defaultValue = "1") int startPage
    ) {
        String prompt = newsService.getKeyword(1L);

        NewsResultResponse response = newsService.getResponse(prompt, startPage, 1L);
        return ResponseEntity.ok(response);
    }
}
