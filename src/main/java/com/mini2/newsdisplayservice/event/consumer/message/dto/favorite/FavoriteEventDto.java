package com.mini2.newsdisplayservice.event.consumer.message.dto.favorite;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteEventDto {
    public static final String Topic = "UserFavoriteInfo";

    
    private String eventId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    private String sourceService;
    private FavoritePayloadDto payload;
}