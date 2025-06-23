package com.mini2.newsdisplayservice.event.consumer;

import com.mini2.newsdisplayservice.event.consumer.message.favorite.FavoriteInfoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessageConsumer {
    @KafkaListener(topics = FavoriteInfoEvent.Topic, properties = {
            JsonDeserializer.VALUE_DEFAULT_TYPE +
                    ":com.mini2.newsdisplayservice.event.consumer.message.user.FavoriteInfoEvent"
    })
    void handleFavoriteInfoEvent(FavoriteInfoEvent event, Acknowledgment ack) {
        log.info("FavoriteInfoEvent 처리. 보낸사람={}", event.getSourceService());

        ack.acknowledge();
    }
}