package com.tikitta.backend.controller;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.userbooking.*;
import com.tikitta.backend.repository.*;
import com.tikitta.backend.service.UserBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import com.tikitta.backend.dto.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/{managerId}/booking")
public class UserBookingController {

    private final UserBookingService userBookingService;
    private final KakaoOauthRepository kakaoOauthRepository;
    private final ShowTimeRepository showTimeRepository;
    private final TicketOptionRepository ticketOptionRepository;

    @GetMapping("/{showId}/reserveInfo") // ◀ 2. 엔드포인트 추가
    public ResponseEntity<ApiResponse<BookingInfoResponse>> getReserveInfo(
            @PathVariable Long showId) {

        BookingInfoResponse data = userBookingService.getBookingInfo(showId);
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    // --- 👇 [페이지 1] 예매 시작 (회차/티켓 선택) ---
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<String>> startBooking(
            @RequestBody BookingDto.StartRequest requestDto,
            HttpSession session) {

        int totalPrice = userBookingService.calculateTotalPrice(
                requestDto.getTicketOptionId(), requestDto.getQuantity());

        BookingDto.SessionInfo sessionDto = new BookingDto.SessionInfo(
                requestDto.getShowtimeId(),
                requestDto.getTicketOptionId(),
                requestDto.getQuantity(),
                totalPrice,
                null, null, null, null,   // 페이지 3 정보
                null, null
        );

        session.setAttribute("currentBooking", sessionDto);

        return ResponseEntity.ok(new ApiResponse<>("예매 정보가 세션에 저장되었습니다. 다음 단계로 진행하세요."));
    }

    // --- 👇 [페이지 3] 예매자 정보 입력 ---
    @PostMapping("/details")
    public ResponseEntity<ApiResponse<String>> saveBookingDetails(
            @RequestBody BookingDto.DetailsRequest requestDto,
            HttpSession session) {

        BookingDto.SessionInfo sessionDto = (BookingDto.SessionInfo) session.getAttribute("currentBooking");
        if (sessionDto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "예매 정보가 만료되었습니다. 처음부터 다시 시도해주세요."));
        }

        sessionDto.setUserPhone(requestDto.getPhone());
        sessionDto.setRefundBank(requestDto.getRefundBank());
        sessionDto.setRefundAccount(requestDto.getRefundAccount());
        sessionDto.setRefundHolder(requestDto.getRefundHolder());

        session.setAttribute("currentBooking", sessionDto);

        return ResponseEntity.ok(new ApiResponse<>("예매자 정보가 저장되었습니다. 최종 확인 페이지로 이동하세요."));
    }

    // --- 👇 [페이지 4] 최종 확인 정보 조회 ---
    @GetMapping("/confirm-info")
    public ResponseEntity<ApiResponse<BookingDto.ConfirmResponse>> getConfirmInfo(
            HttpSession session, Authentication authentication) {

        BookingDto.SessionInfo sessionDto = (BookingDto.SessionInfo) session.getAttribute("currentBooking");
        if (sessionDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예매 정보가 없습니다.");
        }

        ShowTime showTime = showTimeRepository.findById(sessionDto.getShowtimeId()).orElseThrow();
        TicketOption ticketOption = ticketOptionRepository.findById(sessionDto.getTicketOptionId()).orElseThrow();
        Shows show = showTime.getShow(); // ShowTime을 통해 Shows 접근

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        KakaoOauth user = kakaoOauthRepository.findByEmail(email).orElseThrow();
        String userName = user.getName();

        BookingDto.ConfirmResponse confirmDto = new BookingDto.ConfirmResponse(sessionDto, showTime, ticketOption, show, userName);
        return ResponseEntity.ok(new ApiResponse<>(confirmDto));
    }

    // --- 👇 [페이지 4] 최종 예매 확정 ---
    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservationDetail(
            @PathVariable Long reservationId,
            Authentication authentication) { // ◀ 로그인 사용자 확인용

        ReservationDetailResponse data = userBookingService.getReservationDetail(reservationId, authentication);

        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Long>> confirmBooking(
            HttpSession session, Authentication authentication) {

        BookingDto.SessionInfo sessionDto = (BookingDto.SessionInfo) session.getAttribute("currentBooking");
        if (sessionDto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "예매 정보가 만료되었거나 잘못된 요청입니다."));
        }

        Long reservationId = userBookingService.createReservation(sessionDto, authentication).getId();

        session.removeAttribute("currentBooking");

        return ResponseEntity.ok(new ApiResponse<>(reservationId));
    }

    @PostMapping("/{reservationId}/cancel") // 상태 변경이므로 POST 사용
    public ResponseEntity<ApiResponse<String>> requestCancelReservation(
            @PathVariable Long reservationId,
            Authentication authentication) {

        try {
            userBookingService.cancelReservation(reservationId, authentication);
            return ResponseEntity.ok(new ApiResponse<>("예매 취소 요청이 완료되었습니다."));
        } catch (ResponseStatusException e) {
            // Service에서 발생시킨 예외 처리 (예: 400, 404)
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(e.getStatusCode().value(), e.getReason()));
        } catch (AccessDeniedException e) {
            // 접근 권한 예외 처리 (403 Forbidden)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        } catch (Exception e) {
            // 기타 예상치 못한 오류 처리 (500 Internal Server Error)
            log.error("예매 취소 중 오류 발생: Reservation ID {}", reservationId, e); // 로깅 추가 권장
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "예매 취소 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/{showtimeId}/seats")
    public ResponseEntity<ApiResponse<ShowSeatsResponse>> getAvailableSeats(
            @PathVariable Long showtimeId) {

        ShowSeatsResponse data = userBookingService.getShowSeats(showtimeId);

        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @PostMapping("/{showtimeId}/seats/select")
    public ResponseEntity<ApiResponse<String>> selectSeats(
            @PathVariable Long showtimeId,
            @RequestBody BookingDto.SelectSeatsRequest requestDto,
            HttpSession session) {

        BookingDto.SessionInfo sessionDto =
                (BookingDto.SessionInfo) session.getAttribute("currentBooking");

        if (sessionDto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "예매 정보가 없습니다. 처음부터 다시 시도해주세요."));
        }

        if (requestDto.getShowSeatIds() == null ||
                requestDto.getShowSeatIds().size() != sessionDto.getQuantity()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "선택한 좌석 수가 예매 수량과 일치하지 않습니다."));
        }

        try {
            userBookingService.saveSelectedSeats(
                    showtimeId,
                    sessionDto,
                    requestDto.getShowSeatIds()
            );
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(e.getStatusCode().value(), e.getReason()));
        }

        session.setAttribute("currentBooking", sessionDto);

        return ResponseEntity.ok(
                new ApiResponse<>("좌석 선택이 저장되었습니다. 다음 단계로 진행하세요.")
        );
    }

    @PatchMapping("/{reservationId}/seats")
    public ResponseEntity<ApiResponse<String>> changeSeats(
            @PathVariable Long reservationId,
            @RequestBody ChangeSeatsRequest request,
            Authentication authentication) {
        try {
            userBookingService.changeSeats(reservationId, request.getShowSeatIds(), authentication);
            return ResponseEntity.ok(new ApiResponse<>("좌석 변경이 완료되었습니다."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(e.getStatusCode().value(), e.getReason()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        } catch (Exception e) {
            log.error("좌석 변경 중 오류 발생: reservationId={}", reservationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "좌석 변경 중 오류가 발생했습니다."));
        }
    }
}
