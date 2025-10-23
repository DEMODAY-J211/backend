package com.tikitta.backend.dto;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.Reservation;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyReservationItemDto {
    private Long reservationId;
    private Long showtimeId;
    private String showTitle;
    private LocalDateTime showtimeStart;
    private String ticketOptionName;
    private Integer reservationQuantity;
    private String reservationNumber;
    private DomainEnums.ReservationStatus status; // Enum 타입을 그대로 사용
    private String showPosterPicture;


    public MyReservationItemDto(Reservation reservation) {
        this.reservationId = reservation.getId();
        this.showtimeId = reservation.getShowTime().getId();
        this.showTitle = reservation.getShowTime().getShow().getTitle();
        this.showtimeStart = reservation.getShowTime().getStartAt();
        this.ticketOptionName = reservation.getTicketOption().getName();
        this.reservationQuantity = reservation.getQuantity();
        this.reservationNumber = reservation.getReservationNumber();
        this.status = reservation.getStatus();
        this.showPosterPicture = reservation.getShowTime().getShow().getPosterUrl();
    }
}