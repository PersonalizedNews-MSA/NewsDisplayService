package com.mini2.newsdisplayservice.domain.dto.favorite;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LikeEvent {
    private String category;
    private LocalDateTime createdAt;

    public LikeEvent(String category, LocalDateTime createdAt) {
        this.category = category;
        this.createdAt = createdAt;
    }
}
