package com.tikitta.backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", errorCode.getStatus().value());
        body.put("message", errorCode.getMessage());
        body.put("data", null);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
    }
}
