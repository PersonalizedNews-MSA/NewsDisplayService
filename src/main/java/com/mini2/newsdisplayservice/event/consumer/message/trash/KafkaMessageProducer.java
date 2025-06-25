package com.mini2.newsdisplayservice.event.consumer.message.trash;


import com.mini2.newsdisplayservice.event.consumer.message.dto.favorite.FavoriteEventDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageProducer {
    private final KafkaTemplate<String, FavoriteEventDto> kafkaTemplate;

    public KafkaMessageProducer(KafkaTemplate<String, FavoriteEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, FavoriteEventDto eventDto) {
        kafkaTemplate.send(topic, eventDto);
        System.out.println("Kafka 메시지 전송됨 (객체): " + eventDto);
    }
}