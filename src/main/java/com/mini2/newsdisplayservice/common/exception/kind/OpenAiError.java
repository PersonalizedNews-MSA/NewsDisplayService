package com.mini2.newsdisplayservice.common.exception.kind;

import com.mini2.newsdisplayservice.common.exception.ClientError;

public class OpenAiError extends ClientError {
    public OpenAiError(String message) {
        this.errorCode = "ApiError";
        this.errorMessage = message;
    }
}
