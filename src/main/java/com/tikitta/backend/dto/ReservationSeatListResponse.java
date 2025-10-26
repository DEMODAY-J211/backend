package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ReservationItem 단위로 좌석/입장 현황 반환
 * DTO는 단순 데이터 전달용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationSeatListResponse {

    private Long reservationItemId;
    private Long reservationId;
    private Long userId;
    private String userName;
    private String phone;
    private String seat;           // 좌석 번호, 스탠딩이면 null
    private Long ticketOptionId;
    private boolean isEntered;     // 입장 여부
    private boolean isReserved;    // 좌석 점유 여부
    private LocalDateTime reservationTime;
}
