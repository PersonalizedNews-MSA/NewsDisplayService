package com.mini2.newsdisplayservice.event.consumer.message.dto.interest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterestPayloadDto {
    private List<String> name;

    private String userId;
}