package com.tikitta.backend.dto;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.Shows;
import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.domain.TicketOption;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 예매 관련 DTO들을 모아놓은 클래스
 */
public class BookingDto {

    /**
     * 페이지 1 (회차/티켓 선택) 요청 DTO
     * POST /user/booking/start
     */
    @Getter
    @NoArgsConstructor
    public static class StartRequest {
        private Long showtimeId;
        private Long ticketOptionId;
        private Integer quantity;
    }

    /**
     * 세션에 저장될 예매 진행 정보 DTO
     */
    @Getter
    @Setter
    @AllArgsConstructor
    public static class SessionInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        // --- 페이지 1 정보 ---
        private Long showtimeId;
        private Long ticketOptionId;
        private Integer quantity;
        private Integer calculatedTotalPrice;

        // --- 페이지 3 정보 ---
        private String userPhone;
        private String refundBank;
        private String refundAccount;
        private String refundHolder;

        // 좌석제용 (지금은 null)
        // private List<Long> selectedShowSeatIds;
    }

    /**
     * 페이지 3 (예매자 정보 입력) 요청 DTO
     * POST /user/booking/details
     */
    @Getter
    @NoArgsConstructor
    public static class DetailsRequest {
        private String phone;
        private String refundBank;
        private String refundAccount;
        private String refundHolder;
    }

    /**
     * 페이지 4 (최종 확인) 응답 DTO
     * GET /user/booking/confirm-info
     */
    @Getter
    public static class ConfirmResponse {
        // --- 페이지 3 정보 ---
        private String showTitle;
        private LocalDateTime showtimeStart;
        private String ticketOptionName;
        private Integer quantity;
        private Integer totalPrice;
        // private List<String> selectedSeats; // 좌석제용

        // --- 페이지 4 정보 ---
        private String userName;
        private DomainEnums.Bank managerBankName;
        private String managerAccountNumber;
        private String managerDepositorName;

        public ConfirmResponse(SessionInfo sessionDto, ShowTime showTime, TicketOption ticketOption, Shows show, String userName) {
            this.showTitle = show.getTitle();
            this.showtimeStart = showTime.getStartAt();
            this.ticketOptionName = ticketOption.getName();
            this.quantity = sessionDto.getQuantity();
            this.totalPrice = sessionDto.getCalculatedTotalPrice();
            this.userName = userName;
            this.managerBankName = show.getBankName();
            this.managerAccountNumber = show.getBankAccountNumber();
            this.managerDepositorName = show.getBankDepositorName();
        }
    }
}