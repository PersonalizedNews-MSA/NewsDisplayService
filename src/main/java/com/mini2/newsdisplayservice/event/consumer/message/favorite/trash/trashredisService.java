package com.mini2.newsdisplayservice.event.consumer.message.favorite.trash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class trashredisService {
    private final StringRedisTemplate stringRedisTemplate;

    public Set<String> getUserBookmarks(String userId) {
        String redisKey = "user:" + userId + ":favorites";
        Set<String> bookmarks = stringRedisTemplate.opsForSet().members(redisKey);
        log.info("Redis에서 북마크 조회: Key={}, 북마크 수={}", redisKey, bookmarks != null ? bookmarks.size() : 0);
        return bookmarks;
    }

    // 다른 비즈니스 로직에서 이 메서드를 호출하여 북마크 정보를 가져올 수 있습니다.
    public void exampleUsageOfBookmarks(String userId) {
        Set<String> bookmarks = getUserBookmarks(userId);
        if (bookmarks != null && !bookmarks.isEmpty()) {
            log.info("사용자 {}의 favorite 목록: {}", userId, bookmarks);
            // 가져온 북마크 목록을 사용하여 추가적인 로직 수행
        } else {
            log.info("사용자 {}의 favorite가 없습니다.", userId);
        }
    }
}
