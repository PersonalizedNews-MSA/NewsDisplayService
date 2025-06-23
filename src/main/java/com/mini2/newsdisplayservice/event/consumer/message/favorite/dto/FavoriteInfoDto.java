package com.mini2.newsdisplayservice.event.consumer.message.favorite.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class FavoriteInfoDto {
    private String news_category;
    private LocalDateTime created_time;
}
