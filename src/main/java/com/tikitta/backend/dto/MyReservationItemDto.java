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
    private String showPosterPicture;
    private String reservationStatus;


    public MyReservationItemDto(Reservation reservation) {
        this.reservationId = reservation.getId();
        this.showtimeId = reservation.getShowTime().getId();
        this.showTitle = reservation.getShowTime().getShow().getTitle();
        this.showtimeStart = reservation.getShowTime().getStartAt();
        this.ticketOptionName = reservation.getTicketOption().getName();
        this.reservationQuantity = reservation.getQuantity();
        this.reservationNumber = reservation.getReservationNumber();
        this.showPosterPicture = reservation.getShowTime().getShow().getPosterUrl();
        this.reservationStatus = reservation.getStatus().name();

    }
}