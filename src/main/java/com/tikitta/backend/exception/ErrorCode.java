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
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "This ticket has already been checked in."),

    NOT_EXIST_FILE_EXTENSION(HttpStatus.BAD_REQUEST,"파일 이름이 정확하지 않습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST,"이미지 파일 형태가 잘못되었습니다."),
    NOT_EXIST_FILE(HttpStatus.NOT_FOUND,"파일이 전송되지 않았습니다."),
    IO_EXCEPTION_UPLOAD_FILE(HttpStatus.INTERNAL_SERVER_ERROR,"업로드 중 오류 발생");

    private final HttpStatus status;
    private final String message;


}
