package com.mini2.newsdisplayservice.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini2.newsdisplayservice.event.consumer.message.dto.favorite.FavoriteEventDto;
import com.mini2.newsdisplayservice.event.consumer.message.dto.favorite.FavoriteNewsInfoDto;
import com.mini2.newsdisplayservice.event.consumer.message.dto.favorite.FavoritePayloadDto;
import com.mini2.newsdisplayservice.event.consumer.message.dto.interest.InterestEventDto;
import com.mini2.newsdisplayservice.event.consumer.message.dto.interest.InterestPayloadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessageConsumer {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = FavoriteEventDto.Topic, properties = {
            JsonDeserializer.VALUE_DEFAULT_TYPE +
                    ":com.mini2.newsdisplayservice.event.consumer.message.dto.favorite.FavoriteEventDto"
    })
    void handleFavoriteInfoEvent(FavoriteEventDto event, Acknowledgment ack) {

        if (event == null || event.getPayload() == null) {
            log.warn("수신된 FavoriteInfoEvent가 null이거나 payload가 없습니다. 메시지: {}", event);
            ack.acknowledge(); // 유효하지 않은 메시지라도 일단 처리 완료로 간주
            return;
        }

        FavoritePayloadDto payload = event.getPayload();
        String userId = payload.getUserId();
        String newsId = payload.getNewsId();
        String eventType = event.getEventId();
        String newsCategory = payload.getNewsCategory();
        LocalDateTime createdTime = payload.getCreatedTime();

        log.info("FavoriteInfoEvent 처리. 보낸사람={}, 좋아요카테고리 = {}", event.getSourceService(), payload.getUserId());
        log.info("제발되라제발 userId={}, newsId={}, eventType={}, newsCategory={}, createdTime={}", userId, newsId, eventType, newsCategory, createdTime);

        String userFavoritesRedisKey = "user:" + userId + ":favorites"; // 사용자 즐겨찾기 Set 키
        String newsDetailRedisKey = "news:" + newsId; // 뉴스 상세 정보 String 키

        // 이벤트 타입에 따라 Redis Set에 추가 또는 제거
        if ("좋아요 등록".equalsIgnoreCase(eventType)) {
            // 1. user:{userId}:favorites Set에 newsId 추가
            stringRedisTemplate.opsForSet().add(userFavoritesRedisKey, newsId);
            log.info("Redis에 북마크(하트) 추가됨: Key={}, NewsId={}", userFavoritesRedisKey, newsId);

            // 2. news:{newsId} String에 뉴스 상세 정보(JSON) 저장 또는 업데이트
            try {
                FavoriteNewsInfoDto newsDtoToStore = new FavoriteNewsInfoDto();
                newsDtoToStore.setNewsId(newsId);
                newsDtoToStore.setNewsCategory(newsCategory);
                newsDtoToStore.setCreatedTime(createdTime);


                // FavoriteNewsInfoDto 객체를 JSON 문자열로 변환
                String newsJson = objectMapper.writeValueAsString(newsDtoToStore);
                stringRedisTemplate.opsForValue().set(newsDetailRedisKey, newsJson);

                log.info("Redis에 뉴스 상세 정보 JSON 저장됨: Key={}, JSON={}", newsDetailRedisKey, newsJson);

            } catch (JsonProcessingException e) {
                log.error("뉴스 ID {}의 상세 정보 JSON 직렬화 오류: 오류={}", newsId, e.getMessage(), e);
            }

        } else if ("좋아요 취소".equalsIgnoreCase(eventType)) {
            stringRedisTemplate.opsForSet().remove(userFavoritesRedisKey, newsId);
            log.info("Redis에서 북마크(하트) 제거됨: Key={}, NewsId={}", userFavoritesRedisKey, newsId);
        } else {
            log.warn("알 수 없는 북마크 이벤트 타입: {}. Payload: {}", eventType, payload);
        }

        ack.acknowledge();
    }

    //    interest 받기
    @KafkaListener(topics = InterestEventDto.Topic, properties = {
            JsonDeserializer.VALUE_DEFAULT_TYPE +
                    ":com.mini2.newsdisplayservice.event.consumer.message.dto.interest.InterestEventDto" // 새 DTO 타입 지정
    })
    void handleAnotherEvent(InterestEventDto event, Acknowledgment ack) {
        log.info("새로운 InterestEventDto 토픽 메시지 수신: {}", event);

        if (event == null || event.getPayload() == null) {
            log.warn("수신된 InterestEventDto null이거나 payload가 없습니다. 메시지: {}", event);
            ack.acknowledge();
            return;
        }

        InterestPayloadDto payload = event.getPayload(); // 새로운 페이로드 타입

        List<String> interestList = payload.getName();
        String userId = payload.getUserId();

        log.info("InterestEventDto 처리. userId={}, 리스트={}", userId, interestList);

        // Redis에 가장 최근 목록만 저장 (String 타입, JSON 직렬화)
        // 키는 user:[userId]:latest_interests 와 같이 명확하게 지정
        String userLatestInterestsRedisKey = "user:" + userId + ":latest_interests";

        try {
            if (interestList != null && !interestList.isEmpty()) {
                String jsonList = objectMapper.writeValueAsString(interestList);
                // Redis SET 명령어를 사용하여 이전 값을 덮어쓰고 최신 값으로 업데이트
                stringRedisTemplate.opsForValue().set(userLatestInterestsRedisKey, jsonList);
                log.info("Redis에 사용자 {}의 가장 최근 관심사 목록 JSON이 저장됨: Key={}, JSON={}", userId, userLatestInterestsRedisKey, jsonList);
            } else {
                stringRedisTemplate.delete(userLatestInterestsRedisKey);
                log.info("사용자 {}의 최신 관심사 목록이 비어 있으므로 Redis에서 해당 키를 삭제했습니다.", userId);
            }
        } catch (JsonProcessingException e) {
            log.error("관심사 목록 JSON 직렬화 오류: userId={}, 오류={}", userId, e.getMessage(), e);
        }


        ack.acknowledge();
    }
}