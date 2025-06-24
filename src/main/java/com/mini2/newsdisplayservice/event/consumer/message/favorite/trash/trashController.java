package com.mini2.newsdisplayservice.event.consumer.message.favorite.trash;

import com.mini2.newsdisplayservice.event.consumer.message.favorite.FavoriteInfoEvent;
import com.mini2.newsdisplayservice.event.consumer.message.favorite.dto.FavoriteNewsInfoDto;
import com.mini2.newsdisplayservice.event.consumer.message.favorite.service.FavoriteNewsInfoService;
import com.mini2.newsdisplayservice.event.consumer.message.favorite.trash.KafkaMessageProducer; // 이전에 만든 KafkaMessageProducer 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime; // LocalDateTime 사용을 위해 임포트
import java.util.List;
import java.util.Set;


@Slf4j // Lombok을 사용하여 로그 객체를 자동 생성
@RestController // RESTful API 컨트롤러임을 선언
@RequiredArgsConstructor // Lombok을 사용하여 final 필드를 인자로 받는 생성자를 자동 생성
@RequestMapping("/kafka/test") // 이 컨트롤러의 기본 URL 경로 설정
public class trashController {

    // 이전에 만든 KafkaMessageProducer를 주입받음
    private final KafkaMessageProducer kafkaMessageProducer;
    private final FavoriteNewsInfoService favoriteNewsInfoService;

    @GetMapping("/consumor") // 기존 요청에 따라 이름은 유지, 하지만 역할에 맞게 변경 고려
    public ResponseEntity<List<FavoriteNewsInfoDto>> getBookmarksFromRedis(@RequestParam("userId") Long userId) {
        try {
            log.info("Redis에서 북마크 조회 요청 받음: userId={}", userId);
            // trashredisService의 getTop10Bookmarks 메서드 호출
            List<FavoriteNewsInfoDto> top10Bookmarks = favoriteNewsInfoService.getTop10Favoriets(userId);

            if (top10Bookmarks != null && !top10Bookmarks.isEmpty()) {
                log.info("사용자 {}의 북마크를 성공적으로 조회했습니다. 북마크 수: {}", userId, top10Bookmarks.size());
                return new ResponseEntity<>(top10Bookmarks, HttpStatus.OK);
            } else {
                log.info("사용자 {}의 북마크가 Redis에 없거나 비어 있습니다.", userId);
                // 북마크가 없을 경우 빈 리스트 반환
                return new ResponseEntity<>(List.of(), HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Redis 북마크 조회 실패: userId={}, 오류: {}", userId, e.getMessage(), e);
            // 500 에러와 함께 오류 메시지를 반환합니다.
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendKafkaMessage(@RequestBody FavoriteInfoEvent.Payload payload) {
        try {
            // FavoriteInfoEvent 객체를 생성하고 Payload를 설정
            FavoriteInfoEvent event = new FavoriteInfoEvent();
            event.setEventId("좋아요_등록");
            event.setTimestamp(LocalDateTime.now());
            event.setSourceService("test-api-service"); // 이 API를 통해 보냈음을 나타냄
            event.setPayload(payload); // 요청 본문에서 받은 payload 설정

            // KafkaMessageProducer를 사용하여 메시지 전송
            kafkaMessageProducer.send(FavoriteInfoEvent.Topic, event);

            log.info("Kafka 메시지 전송 요청 성공: {}", event);
            return new ResponseEntity<>("Kafka 메시지 전송 성공", HttpStatus.OK);

        } catch (Exception e) {
            log.error("Kafka 메시지 전송 요청 실패: {}", e.getMessage(), e);
            return new ResponseEntity<>("Kafka 메시지 전송 실패: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
