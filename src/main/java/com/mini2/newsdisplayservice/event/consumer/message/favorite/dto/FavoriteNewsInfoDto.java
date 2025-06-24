package com.mini2.newsdisplayservice.event.consumer.message.favorite.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FavoriteNewsInfoDto {
    private String newsId;
    private String news_category;
    private String created_time;

}
