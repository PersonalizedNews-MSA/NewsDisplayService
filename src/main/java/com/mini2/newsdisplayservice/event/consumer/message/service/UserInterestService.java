package com.mini2.newsdisplayservice.event.consumer.message.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInterestService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public List<String> getUserLatestInterests(Long userId) {
        String redisKey = "user:" + userId.toString() + ":latest_interests";

        log.info("Redis에서 사용자 {}의 관심사 목록 조회 시도. 키: {}", userId, redisKey);
        String jsonList = stringRedisTemplate.opsForValue().get(redisKey);

        if (jsonList == null || jsonList.isEmpty()) {
            log.info("Redis에 사용자 {}의 관심사 목록이 없거나 비어 있습니다. 키: {}", userId, redisKey);
            return new ArrayList<>(); // 데이터가 없으면 빈 리스트 반환
        }

        try {
            List<String> interestList = objectMapper.readValue(jsonList, new TypeReference<List<String>>() {});
            log.info("Redis에서 사용자 {}의 관심사 목록을 성공적으로 조회했습니다. 목록 수: {}", userId, interestList.size());
            return interestList;
        } catch (JsonProcessingException e) {
            log.error("사용자 {}의 관심사 목록 JSON 역직렬화 오류: JSON={}, 오류={}", userId, jsonList, e.getMessage(), e);
            return new ArrayList<>(); // 역직렬화 오류 발생 시 빈 리스트 반환
        }
    }
}
