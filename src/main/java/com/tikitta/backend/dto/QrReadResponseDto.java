package com.tikitta.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class QrReadResponseDto {
    private Long reservationId;
    private Long reservationItemId;
    private Long showtimeId;
    private Long userId;
    private String name;
    private String ticketOption;
    private String seat;
    private boolean isEntered;
    private boolean isAvailable;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime checkinTime;

    public static QrReadResponseDto from(ReservationItem item, boolean isSeatAvailable) {
        String seatInfo = item.getShowSeat() != null ? 
                item.getShowSeat().getSeat().getSeatRow() + "-" + item.getShowSeat().getSeat().getSeatNumber() :
                "Standing: " + item.getEntryNumber();

        return QrReadResponseDto.builder()
                .reservationId(item.getReservation().getId())
                .reservationItemId(item.getId())
                .showtimeId(item.getReservation().getShowTime().getId())
                .userId(item.getReservation().getUser().getId())
                .name(item.getReservation().getUser().getProfile().getNickname())
                .ticketOption(item.getReservation().getTicketOption().getName())
                .seat(seatInfo)
                .isEntered(item.isEntered())
                .isAvailable(isSeatAvailable) // ShowSeat의 최종 상태
                .checkinTime(item.getEnteredAt())
                .build();
    }
}
