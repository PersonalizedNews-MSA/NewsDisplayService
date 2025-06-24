package com.mini2.newsdisplayservice.event.consumer;

import com.mini2.newsdisplayservice.domain.service.NewsService;
import com.mini2.newsdisplayservice.event.consumer.message.favorite.FavoriteInfoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessageConsumer {
    private final StringRedisTemplate stringRedisTemplate;
//    private final NewsService newsService;

    @KafkaListener(topics = FavoriteInfoEvent.Topic, properties = {
            JsonDeserializer.VALUE_DEFAULT_TYPE +
                    ":com.mini2.newsdisplayservice.event.consumer.message.favorite.FavoriteInfoEvent"
    })
    void handleFavoriteInfoEvent(FavoriteInfoEvent event, Acknowledgment ack) {
        log.warn("크아악 창섭이형");
        if(event==null || event.getPayload()==null) {
            log.warn("수신된 FavoriteInfoEvent가 null이거나 payload가 없습니다. 메시지: {}", event);
            ack.acknowledge(); // 유효하지 않은 메시지라도 일단 처리 완료로 간주
            return;
        }

        FavoriteInfoEvent.Payload payload = event.getPayload();
        String userId = payload.getUserId();
        String newsId = payload.getNewsId();
        String eventType = event.getEventId();
        String newsCategory = payload.getNewsCategory();
        LocalDateTime createdTime = payload.getCreatedTime();

        log.info("FavoriteInfoEvent 처리. 보낸사람={}, 좋아요카테고리 = {}", event.getSourceService(), payload.getUserId());
        log.info("제발되라제발 userId={}, newsId={}, eventType={}, newsCategory={}, createdTime={}", userId, newsId, eventType, newsCategory, createdTime);

        // Redis Key: user:{userId}:좋아요
        String redisKey = "user:" + userId + ":favorites";

        // 이벤트 타입에 따라 Redis Set에 추가 또는 제거
        if ("좋아요 등록".equalsIgnoreCase(eventType)) { // 대소문자 무시
            stringRedisTemplate.opsForSet().add(redisKey, newsId);
            log.info("Redis에 북마크(하트) 추가됨: Key={}, NewsId={}", redisKey, newsId);
        } else if ("좋아요 취소".equalsIgnoreCase(eventType)) { // 대소문자 무시
            stringRedisTemplate.opsForSet().remove(redisKey, newsId);
            log.info("Redis에서 북마크(하트) 제거됨: Key={}, NewsId={}", redisKey, newsId);
        } else {
            log.warn("알 수 없는 북마크 이벤트 타입: {}. Payload: {}", eventType, payload);
        }

        ack.acknowledge();
    }
}