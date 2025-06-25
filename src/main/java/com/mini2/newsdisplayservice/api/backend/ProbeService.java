package com.mini2.newsdisplayservice.api.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProbeService {
    public void validateLiveness(){
        // TODO: 만일 서비스를 재시작해야하는 상황이면 exception발생
    }

    public void validateReadiness(){
        // TODO: 만일 서비스 수행을 일시 중지해야하는 상황잉면 exception발생
    }
}
