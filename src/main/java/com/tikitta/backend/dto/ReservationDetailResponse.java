package com.tikitta.backend.dto;

import com.tikitta.backend.domain.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ReservationDetailResponse {

    private String showTitle;
    private String showPoster;
    private LocalDateTime showtimeStart;
    private String showLocation;
    private String reservationNumber;
    private List<String> seatList; // 좌석 번호 리스트 (예: ["A1", "A2"]) 또는 스탠딩 입장 번호 리스트
    private String userName;
    private String userPhone;
    private LocalDateTime reservationDate;
    private String ticketOptionName; // 예매 시 선택한 티켓 옵션 이름
    private Integer price;          // 예매 시 선택한 티켓 옵션 가격
    private Integer quantity;
    private DomainEnums.ReservationStatus reservationstatus; // JSON 키 소문자 유지
    private Integer totalAmount; // JSON 키 오타 수정 (totalAmount -> totalPrice)
    private RefundInfo refundInfo;

    public ReservationDetailResponse(Reservation reservation, TicketOption selectedTicketOption) {
        ShowTime showTime = reservation.getShowTime();
        Shows show = showTime.getShow();
        KakaoOauth user = reservation.getUser();

        this.showTitle = show.getTitle();
        this.showPoster = show.getPosterUrl();
        this.showtimeStart = showTime.getStartAt();
        this.showLocation = show.getLocation().getName();
        this.reservationNumber = reservation.getReservationNumber();
        this.userName = user.getName();
        this.userPhone = user.getPhone(); // KakaoOauth에 phone 필드 사용
        this.reservationDate = reservation.getCreatedAt();
        this.quantity = reservation.getQuantity();
        this.reservationstatus = reservation.getStatus();
        this.totalAmount = reservation.getTotalPrice(); // 필드 이름 totalPrice 사용

        // 예매 시 사용된 TicketOption 정보 설정
        if (selectedTicketOption != null) {
            this.ticketOptionName = selectedTicketOption.getName();
            this.price = selectedTicketOption.getPrice();
        } else {
            // 예외 처리 또는 기본값 설정 (ReservationItem에서 찾아야 할 수도 있음)
            this.ticketOptionName = "정보 없음";
            this.price = 0;
        }

        // 좌석 리스트 또는 입장 번호 리스트 추출
        if (show.getSaleMethod() == DomainEnums.SaleMethod.Select_by_User) {
            // 좌석제: ReservationItem -> ShowSeat -> Seat -> seatNumber
            this.seatList = reservation.getReservationItems().stream()
                    .map(item -> item.getShowSeat().getSeat().getSeatNumber())
                    .collect(Collectors.toList());
        } else {
            // 스탠딩/기타: ReservationItem -> entryNumber
            this.seatList = reservation.getReservationItems().stream()
                    .map(item -> "입장번호 " + item.getEntryNumber())
                    .collect(Collectors.toList());
        }

        // 환불 정보 파싱 (공백 기준으로 분리)
        this.refundInfo = new RefundInfo(reservation.getRefundAccountNumber());
    }

    /**
     * 환불 정보를 담는 중첩 DTO
     */
    @Getter
    private static class RefundInfo {
        private String refundBank;
        private String refundAccount;
        private String refundHolder;

        public RefundInfo(String fullRefundInfo) {
            if (fullRefundInfo != null && !fullRefundInfo.isEmpty()) {
                String[] parts = fullRefundInfo.split(" ");
                if (parts.length >= 3) {
                    this.refundBank = parts[0];
                    this.refundAccount = parts[1];
                    this.refundHolder = parts[2];
                }
                // TODO: 파싱 실패 시 예외 처리 또는 기본값 설정
            }
        }
    }
}