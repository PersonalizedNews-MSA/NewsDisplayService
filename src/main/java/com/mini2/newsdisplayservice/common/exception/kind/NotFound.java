package com.mini2.newsdisplayservice.common.exception.kind;

import com.mini2.newsdisplayservice.common.exception.ClientError;

public class NotFound extends ClientError {
    public NotFound(String message) {
        this.errorCode = "NotFound";
        this.errorMessage = message;
    }
}
