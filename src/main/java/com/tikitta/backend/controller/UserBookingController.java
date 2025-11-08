package com.tikitta.backend.controller;

import com.tikitta.backend.domain.*;
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
            @PathVariable Long managerId,
            @PathVariable Long showId) {

        // 3. Service 호출 및 ApiResponse로 감싸서 반환
        BookingInfoResponse data = userBookingService.getBookingInfo(showId);
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    // --- 👇 [페이지 1] 예매 시작 (회차/티켓 선택) ---
    // URL: POST /user/booking/start
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<String>> startBooking(
            @RequestBody BookingDto.StartRequest requestDto,
            HttpSession session) {

        // 1. 총 가격 계산
        int totalPrice = userBookingService.calculateTotalPrice(requestDto.getTicketOptionId(), requestDto.getQuantity());

        // 2. 세션에 저장할 DTO 생성
        BookingDto.SessionInfo sessionDto = new BookingDto.SessionInfo(
                requestDto.getShowtimeId(),
                requestDto.getTicketOptionId(),
                requestDto.getQuantity(),
                totalPrice,
                null, null, null, null // 페이지 3 정보는 아직 null
        );

        // 3. 세션에 "currentBooking" 이름으로 저장
        session.setAttribute("currentBooking", sessionDto);

        // TODO: 좌석제일 경우 좌석 선택 페이지로, 아니면 바로 페이지 3으로 이동 응답
        return ResponseEntity.ok(new ApiResponse<>("예매 정보가 세션에 저장되었습니다. 다음 단계로 진행하세요."));
    }

    // --- 👇 [페이지 3] 예매자 정보 입력 ---
    // URL: POST /user/booking/details
    @PostMapping("/details")
    public ResponseEntity<ApiResponse<String>> saveBookingDetails(
            @RequestBody BookingDto.DetailsRequest requestDto,
            HttpSession session) {

        // 1. 세션에서 기존 정보 가져오기
        BookingDto.SessionInfo sessionDto = (BookingDto.SessionInfo) session.getAttribute("currentBooking");
        if (sessionDto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "예매 정보가 만료되었습니다. 처음부터 다시 시도해주세요."));
        }

        // 2. 세션 정보 업데이트
        sessionDto.setUserPhone(requestDto.getPhone());
        sessionDto.setRefundBank(requestDto.getRefundBank());
        sessionDto.setRefundAccount(requestDto.getRefundAccount());
        sessionDto.setRefundHolder(requestDto.getRefundHolder());

        // 3. 업데이트된 정보로 세션 덮어쓰기
        session.setAttribute("currentBooking", sessionDto);

        return ResponseEntity.ok(new ApiResponse<>("예매자 정보가 저장되었습니다. 최종 확인 페이지로 이동하세요."));
    }

    // --- 👇 [페이지 4] 최종 확인 정보 조회 ---
    // URL: GET /user/booking/confirm-info
    @GetMapping("/confirm-info")
    public ResponseEntity<ApiResponse<BookingDto.ConfirmResponse>> getConfirmInfo(
            HttpSession session, Authentication authentication) {

        // 1. 세션 정보 가져오기
        BookingDto.SessionInfo sessionDto = (BookingDto.SessionInfo) session.getAttribute("currentBooking");
        if (sessionDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예매 정보가 없습니다.");
        }

        // 2. 관련 엔티티 정보 조회 (DB 접근 최소화)
        ShowTime showTime = showTimeRepository.findById(sessionDto.getShowtimeId()).orElseThrow();
        TicketOption ticketOption = ticketOptionRepository.findById(sessionDto.getTicketOptionId()).orElseThrow();
        Shows show = showTime.getShow(); // ShowTime을 통해 Shows 접근

        // 3. 로그인 사용자 이름 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        KakaoOauth user = kakaoOauthRepository.findByEmail(email).orElseThrow();
        String userName = user.getName();

        // 4. 응답 DTO 생성 및 반환
        BookingDto.ConfirmResponse confirmDto = new BookingDto.ConfirmResponse(sessionDto, showTime, ticketOption, show, userName);
        return ResponseEntity.ok(new ApiResponse<>(confirmDto));
    }

    // --- 👇 [페이지 4] 최종 예매 확정 ---
    // URL: POST /user/booking/confirm
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Long>> confirmBooking(
            HttpSession session, Authentication authentication) {

        // 1. 세션 정보 가져오기
        BookingDto.SessionInfo sessionDto = (BookingDto.SessionInfo) session.getAttribute("currentBooking");
        if (sessionDto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "예매 정보가 만료되었거나 잘못된 요청입니다."));
        }

        // 2. Service 호출하여 Reservation 생성
        // @Transactional이 Service에 있으므로 DB 작업은 원자적으로 처리됨
        Long reservationId = userBookingService.createReservation(sessionDto, authentication).getId();

        // 3. (중요) 예매 완료 후 세션 정보 삭제
        session.removeAttribute("currentBooking");

        // 4. 생성된 예매 ID 반환
        return ResponseEntity.ok(new ApiResponse<>(reservationId));
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservationDetail(
            @PathVariable Long reservationId,
            Authentication authentication) { // ◀ 로그인 사용자 확인용

        // 1. Service 호출
        ReservationDetailResponse data = userBookingService.getReservationDetail(reservationId, authentication);

        // 2. ApiResponse로 감싸서 반환
        return ResponseEntity.ok(new ApiResponse<>(data));
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
}
