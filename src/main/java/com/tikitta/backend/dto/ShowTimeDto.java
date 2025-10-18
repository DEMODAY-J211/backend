package com.tikitta.backend.dto;

import com.tikitta.backend.domain.ShowTime;

import java.time.LocalDateTime;

public class ShowTimeDto {
    private Long showtimeId;
    private LocalDateTime showtimeStart;

    public ShowTimeDto(ShowTime showTime){
        this.showtimeId = showTime.getId();
        this.showtimeStart = showTime.getStartAt();
    }
}
