package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckinListResponse {
    private String showTitle;
    private List<CheckinShowTimeDto> showTimeList;
    private LocalDateTime selectedShowTime;
    private Long selectedShowTimeId;
    private List<ReservationDetailDto> reservationList;
}
