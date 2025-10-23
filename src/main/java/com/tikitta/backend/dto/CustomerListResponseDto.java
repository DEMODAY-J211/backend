package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerListResponseDto {
    private List<ShowTimeInfo> showTimeList;
    private LocalDateTime selectedShowTime;
    private Long selectedShowTimeId;
    private String keyword; // 새로 추가된 필드
    private List<ReservationDetailDto> reservationList;
}
