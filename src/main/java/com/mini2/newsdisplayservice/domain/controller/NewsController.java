package com.mini2.newsdisplayservice.domain.controller;


import com.mini2.newsdisplayservice.domain.dto.NewsResponse;
import com.mini2.newsdisplayservice.domain.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "News API", description = "뉴스 조회 및 AI 요약 관련 API")
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "헤드라인 뉴스 목록 조회", description = "최신 헤드라인 뉴스 목록을 조회합니다.")
    @GetMapping("/news")
    public ResponseEntity<List<NewsResponse>> getNews(@AuthenticationPrincipal PrincipalDetails principal) {
        List<NewsResponse> newsList = newsService.getNews(principal.getId());
        return ResponseEntity.ok(newsList);
    }

    @Operation(summary = "키워드 기반 뉴스 검색", description = "사용자가 입력한 키워드로 뉴스 목록을 검색합니다.")
    @GetMapping("/news/search/{keyword}")
    public ResponseEntity<List<NewsResponse>> searchNews(
            @Parameter(description = "검색할 키워드", example = "기후 변화")@PathVariable String keyword,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principal) {
        List<NewsResponse> newsList = newsService.getSearchNews(keyword,principal.getId());
        return ResponseEntity.ok(newsList);
    }

    @Operation(summary = "AI 추천 뉴스 조회", description = "사용자의 관심사 기반 AI 추천 뉴스를 조회합니다.")
    @GetMapping({"/news/ai", "/news/ai/{start}"})
    public ResponseEntity<NewsResultResponse> getPrompt(
            @Parameter(description = "페이징 시작 인덱스 10단위 (선택)", example = "11") @PathVariable(required = false) Integer start,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principal) {
        String prompt = newsService.getKeyword(principal.getId());
        NewsResultResponse response = newsService.getResponse(prompt, start,principal.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "뉴스 링크 본문 AI 요약", description = "뉴스 링크를 AI가 요약하여 제공합니다.")
    @PostMapping("/news/summary")
    public ResponseEntity<NewsSummaryResponse> getSummary(@RequestBody NewsSummaryRequest request){
        System.out.println("뉴스서머리 시작" + request.getLink());
        NewsSummaryResponse response = newsService.getSummary(request.getLink());
        System.out.println("response 호출완려" + response);
        if(response == null) throw new CustomException(ErrorCode.NEWS_PARSING_ERROR);
        System.out.println("response 정상 (null아님");
        
        return ResponseEntity.ok(response);
    }
}
