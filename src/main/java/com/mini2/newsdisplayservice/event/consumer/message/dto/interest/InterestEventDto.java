package com.mini2.newsdisplayservice.event.consumer.message.dto.interest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterestEventDto {
    public static final String Topic = "UserInterestInfo";


    private String eventId;
    private LocalDateTime timestamp;
    private String sourceService;
    private InterestPayloadDto payload;
}