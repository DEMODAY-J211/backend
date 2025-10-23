package com.tikitta.backend.dto;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDetailDto {
    private Long reservationId;
    private Long showtimeId;
    private Long kakaoId;
    private String reservationNumber;
    private String name;
    private String phone;
    private LocalDateTime reservationTime;
    private String status;
    private boolean isReserved;
    private TicketDetailDto detailed;

    public static ReservationDetailDto fromEntity(Reservation reservation) {
        // isReserved: '입금대기' 또는 '입금확인' 상태일 때 true
        boolean reserved = reservation.getStatus() == DomainEnums.ReservationStatus.PENDING_PAYMENT ||
                           reservation.getStatus() == DomainEnums.ReservationStatus.CONFIRMED;

        return new ReservationDetailDto(
                reservation.getId(),
                reservation.getShowTime().getId(),
                reservation.getUser().getId(),
                reservation.getReservationNumber(),
                reservation.getUser().getName(),
                reservation.getUser().getPhone(), // KakaoOauth 엔티티에 getPhone()이 있다고 가정
                reservation.getCreatedAt(), // 예매 시간을 생성 시간으로 가정
                reservation.getStatus().name(), // Enum 이름을 문자열로 변환
                reserved,
                // 이제 reservation.getTicketOption()이 유효하므로 정상적으로 작동합니다.
                new TicketDetailDto(reservation.getTicketOption(), reservation.getQuantity())
        );
    }
}
