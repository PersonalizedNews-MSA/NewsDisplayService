package com.mini2.newsdisplayservice.event.consumer.message.favorite.trash;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.mini2.newsdisplayservice.event.consumer.message.favorite.FavoriteInfoEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageProducer {

    private final KafkaTemplate<String, FavoriteInfoEvent> kafkaTemplate;


    public KafkaMessageProducer(KafkaTemplate<String, FavoriteInfoEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;

    }

    public void send(String topic, FavoriteInfoEvent eventDto) {
        // try-catch (JsonProcessingException)도 필요 없음. KafkaTemplate이 처리 ✅

        kafkaTemplate.send(topic, eventDto); // FavoriteInfoEvent 객체를 직접 보냄
        System.out.println("Kafka 메시지 전송됨 (객체): " + eventDto);
    }
}