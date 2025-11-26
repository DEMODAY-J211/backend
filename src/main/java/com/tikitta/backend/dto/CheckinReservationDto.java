package com.tikitta.backend.dto;

import com.tikitta.backend.domain.ReservationItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinReservationDto {
    private Long reservationItemId;
    private Long reservationId;
    private Long userId;
    private String userName;
    private String phone;
    private String seat;
    private Long ticketOptionId;
    private boolean isEntered;
    private boolean isReserved;
    private LocalDateTime reservationTime;

    public static CheckinReservationDto fromEntity(ReservationItem item) {
        return CheckinReservationDto.builder()
                .reservationItemId(item.getId())
                .reservationId(item.getReservation().getId())
                .userId(item.getReservation().getUser().getId())
                .userName(item.getReservation().getUser().getName())
                .phone(item.getReservation().getUser().getPhoneNumber())
                .seat(item.getSeat().getName())
                .ticketOptionId(item.getTicketOption().getId())
                .isEntered(item.getIsEntered())
                .isReserved(item.getReservation().getStatus() == com.tikitta.backend.domain.DomainEnums.ReservationStatus.CONFIRMED)
                .reservationTime(item.getReservation().getReservationTime())
                .build();
    }
}
