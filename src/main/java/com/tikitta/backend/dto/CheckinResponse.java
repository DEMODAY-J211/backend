package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResponse {
    private String showTitle;
    private List<CheckinShowTimeDto> showTimeList;
    private LocalDateTime selectedShowTime;
    private Long selectedShowTimeId;
    private List<List<Object>> seat;
    private List<CheckinReservationDto> reservation;
}
