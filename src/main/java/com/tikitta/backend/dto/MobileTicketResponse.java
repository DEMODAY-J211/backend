package com.tikitta.backend.dto;

import com.tikitta.backend.domain.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MobileTicketResponse {

    private String reservationNumber;
    private String showTitle;
    private LocalDateTime showDateTime; // 필드 이름 JSON과 일치
    private String showLocation;
    private String userName;
    private TicketOptionInfo ticketOption; // 중첩 DTO 사용
    private List<TicketItemDto> tickets; // 중첩 DTO 리스트 사용

    public MobileTicketResponse(Reservation reservation) {
        ShowTime showTime = reservation.getShowTime();
        Shows show = showTime.getShow();
        KakaoOauth user = reservation.getUser();
        TicketOption option = reservation.getTicketOption();

        this.reservationNumber = reservation.getReservationNumber();
        this.showTitle = show.getTitle();
        this.showDateTime = showTime.getStartAt();
        this.showLocation = show.getLocation().getName();
        this.userName = user.getName();

        // 티켓 옵션 정보 매핑
        if (option != null) {
            this.ticketOption = new TicketOptionInfo(option);
        }

        // 개별 티켓 정보 매핑
        this.tickets = reservation.getReservationItems().stream()
                .map(TicketItemDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 티켓 옵션 정보를 담는 중첩 DTO
     */
    @Getter
    private static class TicketOptionInfo {
        private Long ticketOptionId;
        private String ticketOptionName;
        private Integer ticketOptionPrice;

        public TicketOptionInfo(TicketOption ticketOption) {
            this.ticketOptionId = ticketOption.getId();
            this.ticketOptionName = ticketOption.getName();
            this.ticketOptionPrice = ticketOption.getPrice();
        }
    }

    /**
     * 개별 티켓 정보를 담는 중첩 DTO
     */
    @Getter
    private static class TicketItemDto {
        private Long ReservationItemId; // JSON 키와 일치
        private SeatInfo seat; // 중첩 DTO 사용 (스탠딩일 경우 null)
        private String qrCode; // JSON 키와 일치

        public TicketItemDto(ReservationItem item) {
            this.ReservationItemId = item.getId();
            this.qrCode = item.getQrCodeUrl(); // DB에 저장된 QR 코드 URL

            // 좌석 정보 매핑 (좌석제일 경우에만)
            if (item.getShowSeat() != null && item.getShowSeat().getSeat() != null) {
                this.seat = new SeatInfo(item.getShowSeat().getSeat());
            } else {
                this.seat = null; // 스탠딩 또는 좌석 정보 없는 경우
            }
        }
    }

    /**
     * 좌석 정보를 담는 중첩 DTO
     */
    @Getter
    private static class SeatInfo {
        private Long seatId;
        private String seatNumber;

        public SeatInfo(Seat seat) {
            this.seatId = seat.getId();
            this.seatNumber = seat.getSeatNumber();
        }
    }
}