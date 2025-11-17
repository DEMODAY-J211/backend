package com.tikitta.backend.dto.userbooking;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ShowSeatsResponse {
    private Long showtimeId;
    private LocalDateTime showStartTime;
    private List<SeatResponseDto> seats;
}