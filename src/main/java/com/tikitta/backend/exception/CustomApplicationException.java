package com.tikitta.backend.exception;

public class CustomApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomApplicationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }
}
