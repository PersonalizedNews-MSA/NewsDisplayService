package com.mini2.newsdisplayservice.event.consumer.message.favorite.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini2.newsdisplayservice.event.consumer.message.favorite.dto.FavoriteNewsInfoDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class FavoriteNewsInfoService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public List<FavoriteNewsInfoDto> getTop10Favoriets(Long userId) {
        String redisKey = "user:" + userId + ":favorites";
        Set<String> favoriteNewsIds = stringRedisTemplate
                .opsForSet()
                .members(redisKey);

        List<FavoriteNewsInfoDto> top10News = new ArrayList<>();

        if (favoriteNewsIds != null && !favoriteNewsIds.isEmpty()) { // Set이 비어있지 않은 경우에만 처리
            int count = 0;
            for (String newsId : favoriteNewsIds) {
                if (count >= 10) break; // 최대 10개까지만 가져옴

                String newsKey = "news:" + newsId; // 뉴스 ID를 키로 사용하여 Redis에서 뉴스 정보 조회
                String newsJson = stringRedisTemplate.opsForValue().get(newsKey); // Redis에서 JSON 형태의 뉴스 정보 가져옴

                if (newsJson != null) {
                    try {
                        // JSON 문자열을 FavoriteNewsInfoDto 객체로 변환
                        FavoriteNewsInfoDto newsDto = objectMapper.readValue(newsJson, FavoriteNewsInfoDto.class);
                        top10News.add(newsDto); // 변환된 객체를 리스트에 추가
                        count++;
                    } catch (JsonProcessingException e) {
                        log.error("Redis에서 가져온 뉴스 정보 JSON 파싱 오류: newsId={}, JSON={}, 오류={}", newsId, newsJson, e.getMessage(), e);
                        // 파싱 오류가 발생해도 다른 뉴스 처리는 계속 진행
                    }
                } else {
                    log.warn("Redis에서 뉴스 정보를 찾을 수 없습니다. newsId={}. 북마크에는 있지만 실제 뉴스 정보는 없음.", newsId);
                }
            }
        }
        log.info("사용자 {}의 북마크 뉴스 {}개 조회 (객체 형태)", userId, top10News.size());
        return top10News;
    }

    public void saveBookmark(Long userId, FavoriteNewsInfoDto newsDto) throws JsonProcessingException {
        // user:{userId}:favorites Set에 newsId를 추가
        String userFavoritesKey = "user:" + userId + ":favorites";
        stringRedisTemplate.opsForSet().add(userFavoritesKey, newsDto.getNewsId());

        // news:{newsId} String에 FavoriteNewsInfoDto 객체를 JSON으로 저장
        String newsInfoKey = "news:" + newsDto.getNewsId();
        String newsJson = objectMapper.writeValueAsString(newsDto);
        stringRedisTemplate.opsForValue().set(newsInfoKey, newsJson);

        log.info("사용자 {}의 북마크 저장 (객체): NewsId={}, NewsInfo={}", userId, newsDto.getNewsId(), newsJson);
    }


}
