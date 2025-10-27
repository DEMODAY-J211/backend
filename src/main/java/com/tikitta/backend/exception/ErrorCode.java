package com.tikitta.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 404 NOT FOUND
    QR_NOT_FOUND(HttpStatus.NOT_FOUND, "QR code not found or invalid"),

    // 409 CONFLICT
    SHOWTIME_MISMATCH(HttpStatus.CONFLICT, "Showtime ID mismatch — QR belongs to another performance"),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "This ticket has already been checked in.");

    private final HttpStatus status;
    private final String message;
}
