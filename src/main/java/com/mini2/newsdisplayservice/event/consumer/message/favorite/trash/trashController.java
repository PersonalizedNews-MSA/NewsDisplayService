package com.mini2.newsdisplayservice.event.consumer.message.favorite.trash;

import com.mini2.newsdisplayservice.event.consumer.message.favorite.FavoriteInfoEvent;
import com.mini2.newsdisplayservice.event.consumer.message.favorite.trash.KafkaMessageProducer; // 이전에 만든 KafkaMessageProducer 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime; // LocalDateTime 사용을 위해 임포트

@Slf4j // Lombok을 사용하여 로그 객체를 자동 생성
@RestController // RESTful API 컨트롤러임을 선언
@RequiredArgsConstructor // Lombok을 사용하여 final 필드를 인자로 받는 생성자를 자동 생성
@RequestMapping("/kafka/test") // 이 컨트롤러의 기본 URL 경로 설정
public class trashController {

    // 이전에 만든 KafkaMessageProducer를 주입받음
    private final KafkaMessageProducer kafkaMessageProducer;


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
