package com.mini2.newsdisplayservice.event.consumer.message.favorite;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
public class FavoriteInfoEvent {
    public static final String Topic = "UserFavoriteInfo";

    private String eventId;
    private String timestamp; // LocaldateTime 형태

    private String sourceService;

    private Payload payload;

    @Data // Payload 내부 클래스에도 Lombok 어노테이션 적용
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        @JsonProperty("userId")
        private String userId;

        @JsonProperty("newsId")
        private String newsId;

        @JsonProperty("news_category")
        private String newsCategory;

        @JsonProperty("created_time")
        private String createdTime;
    }
}