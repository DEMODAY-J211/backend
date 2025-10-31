package com.tikitta.backend.dto;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final int code;
    private final String message;
    private final T data;

    // 성공 시
    public ApiResponse(T data) {
        this.success = true;
        this.code = 200;
        this.message = "success";
        this.data = data;
    }

    // 실패 시
    public ApiResponse(int code, String message) {
        this.success = false;
        this.code = code;
        this.message = message;
        this.data = null;
    }

    public ApiResponse(int code, String message, T data) {
        this.success = true;
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
