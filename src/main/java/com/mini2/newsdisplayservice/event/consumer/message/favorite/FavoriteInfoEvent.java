package com.mini2.newsdisplayservice.event.consumer.message.favorite;

import com.mini2.newsdisplayservice.event.consumer.message.favorite.dto.FavoriteInfoDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class FavoriteInfoEvent {
    public static final String Topic = "UserFavoriteInfo";
    private String eventId;
    private LocalDateTime timestamp;
    private String sourceService;

    private List<FavoriteInfoDto> payload;
}