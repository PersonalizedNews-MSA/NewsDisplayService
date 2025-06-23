package com.mini2.newsdisplayservice.common.exception.kind;

import com.mini2.newsdisplayservice.common.exception.ClientError;

public class BadParameter extends ClientError {
    public BadParameter(String message) {
        this.errorCode = "BadParameter";
        this.errorMessage = message;
    }
}
