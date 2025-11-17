package com.tikitta.backend.service;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.userbooking.*;
import com.tikitta.backend.repository.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBookingService {

    private final ShowsRepository showsRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ReservationRepository reservationRepository;
    private final KakaoOauthRepository kakaoOauthRepository;
    private final ShowTimeRepository showTimeRepository;
    private final TicketOptionRepository ticketOptionRepository;
    private final ReservationItemRepository reservationItemRepository;

    public BookingInfoResponse getBookingInfo(Long showId) {
        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다. ID: " + showId));

// 1. 회차별 DTO (ShowTimeItemDto) 리스트 생성
        List<BookingInfoResponse.ShowTimeItemDto> showTimeDtos =
                show.getShowTimes().stream()
                        .map(showTime -> {
                            // 2. 각 회차별로 잔여 좌석 계산
                            int availableSeats = calculateAvailableSeats(showTime);
                            return new BookingInfoResponse.ShowTimeItemDto(showTime, availableSeats);
                        })
                        .collect(Collectors.toList());

        // 3. 최종 DTO 조립
        return new BookingInfoResponse(show, showTimeDtos);
    }

    private int calculateAvailableSeats(ShowTime showTime) {
        Shows show = showTime.getShow();
        DomainEnums.SaleMethod saleMethod = show.getSaleMethod();
        // 예매 완료/대기중인 상태 목록
        List<DomainEnums.ReservationStatus> activeStatuses = List.of(
                DomainEnums.ReservationStatus.CONFIRMED,
                DomainEnums.ReservationStatus.PENDING_PAYMENT
        );

        // 1. 좌석제-직접선택 공연일 경우 (새로운 모델 적용)
        if (saleMethod == DomainEnums.SaleMethod.Select_by_User) {
            // "이 회차"에 할당된 좌석 중 "isAvailable = true"인 좌석 수
            return showSeatRepository.countByShowTimeAndIsAvailable(showTime, true);
        }

        // 2. 그 외 (스탠딩, 스케줄링, 주최자선택) 공연일 경우 (수량 계산)
        else {
            Integer totalQuantity = showTime.getTotalStandingQuantity();
            if (totalQuantity == null || totalQuantity <= 0) {
                return 0;
            }
            List<Reservation> reservations = reservationRepository.findByShowTimeAndStatusIn(showTime, activeStatuses);
            int bookedQuantity = reservations.stream()
                    .mapToInt(Reservation::getQuantity)
                    .sum();
            return Math.max(0, totalQuantity - bookedQuantity); // 음수 방지
        }

    }

    //예매 총 가격 계산
    // 예매 총 가격 계산 (안전하게)
    public int calculateTotalPrice(Long ticketOptionId, Integer quantity) {
        if (ticketOptionId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("티켓 옵션 ID와 수량은 필수입니다.");
        }

        TicketOption ticketOption = ticketOptionRepository.findById(ticketOptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 옵션입니다."));

        Integer price = ticketOption.getPrice();
        if (price == null) {
            throw new IllegalStateException("티켓 옵션의 가격 정보가 없습니다.");
        }

        return price * quantity;
    }

    /**
     * 예매 최종 확정 (Reservation 및 ReservationItem 생성)
     */
    @Transactional
    public Reservation createReservation(BookingDto.SessionInfo sessionDto,
                                         Authentication authentication) {
        /*
        // 1. 로그인된 사용자 정보 조회
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        KakaoOauth user = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 사용자 정보를 찾을 수 없습니다."));

        // 2. 예매하려는 회차 정보 조회
        ShowTime showTime = showTimeRepository.findById(sessionDto.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 회차입니다."));
        TicketOption ticketOption = ticketOptionRepository.findById(sessionDto.getTicketOptionId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 옵션입니다. ID: " + sessionDto.getTicketOptionId()));
*/


        log.info(">>> [createReservation] sessionDto = {}", sessionDto);
        log.info(">>> [createReservation] authentication = {}", authentication);

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.info(">>> [createReservation] oAuth2User attrs = {}", oAuth2User.getAttributes());

        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        log.info(">>> [createReservation] kakaoAccount = {}", kakaoAccount);

        String email = (String) kakaoAccount.get("email");
        log.info(">>> [createReservation] email = {}", email);

        KakaoOauth user = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 사용자 정보를 찾을 수 없습니다."));
        log.info(">>> [createReservation] kakao user id = {}", user.getId());

        ShowTime showTime = showTimeRepository.findById(sessionDto.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 회차입니다."));
        log.info(">>> [createReservation] showTime id = {}", showTime.getId());

        TicketOption ticketOption = ticketOptionRepository.findById(sessionDto.getTicketOptionId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 옵션입니다. ID: " + sessionDto.getTicketOptionId()));
        log.info(">>> [createReservation] ticketOption id = {}", ticketOption.getId());


        Shows show = showTime.getShow();
        DomainEnums.SaleMethod saleMethod = show.getSaleMethod();

        // --- Reservation 저장 ---
        Reservation reservation = Reservation.builder()
                .user(user)
                .reservationNumber(generateReservationNumber(saleMethod, user))
                .showTime(showTime)
                .quantity(sessionDto.getQuantity())
                .totalPrice(sessionDto.getCalculatedTotalPrice())
                .ticketOption(ticketOption)
                .phone(sessionDto.getUserPhone())
                .refundAccountNumber(sessionDto.getRefundBank() + " " +
                        sessionDto.getRefundAccount() + " " +
                        sessionDto.getRefundHolder())
                .status(DomainEnums.ReservationStatus.PENDING_PAYMENT)
                .createdAt(LocalDateTime.now())
                .build();

        reservationRepository.save(reservation);

        List<ReservationItem> items = new ArrayList<>();

        if (saleMethod == DomainEnums.SaleMethod.Select_by_User) {
            // ========== 좌석제 ==========
            List<Long> showSeatIds = sessionDto.getSelectedShowSeatIds();
            List<ShowSeat> showSeats = showSeatRepository.findAllById(showSeatIds);

            for (ShowSeat ss : showSeats) {
                if (!ss.getShowTime().getId().equals(showTime.getId())) {
                    throw new IllegalStateException("다른 회차의 좌석 포함됨");
                }
                if (!ss.isAvailable()) {
                    throw new IllegalStateException("이미 예약된 좌석 포함됨");
                }

                ss.reserve();
                showSeatRepository.save(ss); // 좌석 점유 반영!

                items.add(ReservationItem.builder()
                        .reservation(reservation)
                        .showSeat(ss)
                        .build());
            }

        } else {
            // ========== 스탠딩 ==========
            Integer maxEntry = reservationItemRepository.findMaxEntryNumberByShowTime(showTime);
            int nextEntry = (maxEntry != null) ? maxEntry + 1 : 1;

            for (int i = 0; i < sessionDto.getQuantity(); i++) {
                items.add(ReservationItem.builder()
                        .reservation(reservation)
                        .entryNumber(nextEntry + i)
                        .build());
            }
        }

        reservationItemRepository.saveAll(items);

        return reservation;
    }

    /**
     * 예매 번호 생성 헬퍼 메소드 (알파벳2 + yymmddHHmm + userId)
     */
    private String generateReservationNumber(DomainEnums.SaleMethod saleMethod, KakaoOauth user) {
        // 1. SaleMethod에 따른 접두사 결정
        String prefix = switch (saleMethod) {
            case Event_Host -> "EH";
            case SCHEDULING -> "SD";
            case STANDING -> "ST";
            case Select_by_User -> "US";
            default -> "XX"; // 예외 처리 또는 기본값
        };

        // 2. 현재 날짜와 시간 (yyMMddHHmm 형식)
        String dateTimePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"));

        // 3. 사용자 ID (KakaoOauth의 Long id 사용)
        String userIdPart = String.valueOf(user.getId());

        return prefix + dateTimePart + userIdPart; // 예: "US2510222015" + "1" -> "US25102220151"
    }

    public ReservationDetailResponse getReservationDetail(Long reservationId, Authentication authentication) {

        // 1. 예매 정보 조회 (Fetch Join으로 연관 엔티티 함께 로드)
        Reservation reservation = reservationRepository.findByIdWithDetails(reservationId) // ◀ Repository에 새 메소드 필요
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다. ID: " + reservationId));

        // 2. 접근 권한 확인 (로그인한 사용자의 예매인지)
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        if (!reservation.getUser().getEmail().equals(email)) {
            throw new SecurityException("자신의 예매 내역만 조회할 수 있습니다.");
        }


        // 3. DTO로 변환하여 반환
        return new ReservationDetailResponse(reservation);
    }

    @Transactional
    public void cancelReservation(Long reservationId, Authentication authentication) {

        // 1. 예매 정보 조회
        Reservation reservation = reservationRepository.findById(reservationId) // Fetch Join 불필요
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 예매입니다. ID: " + reservationId));

        // 2. 접근 권한 확인 (로그인한 사용자의 예매인지)
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        if (!reservation.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("자신의 예매 내역만 취소할 수 있습니다.");
        }

        // 3. 예매 취소 요청 처리 (Reservation 엔티티의 메소드 호출)
        boolean success = reservation.requestCancellation();

        if (!success) {
            // 이미 취소되었거나 취소 불가능한 상태일 경우 예외 발생
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 취소되었거나 취소할 수 없는 예매입니다.");
        }

        // @Transactional에 의해 변경된 reservation 상태가 자동으로 DB에 반영(저장)됩니다.
        log.info("예매 취소 요청 완료: Reservation ID={}, New Status={}", reservationId, reservation.getStatus());

        // --- 스탠딩 공연 ---
        // 스탠딩 공연은 취소 시 별도의 좌석/수량 복구 로직이 당장은 필요 없습니다.
        // 입장 번호는 그대로 유지되고, 잔여석 계산 시 CANCELLED 상태는 제외됩니다.

        // --- 좌석제 공연 (Select_by_User) ---

        if (reservation.getShowTime().getShow().getSaleMethod() == DomainEnums.SaleMethod.Select_by_User) {
            reservation.getReservationItems().forEach(item -> {
                if (item.getShowSeat() != null) {
                    item.getShowSeat().cancel(); // isAvailable = true
                }
            });
        }

    }

    @Transactional(readOnly = true)
    public ShowSeatsResponse getShowSeats(Long showtimeId) {

        // 1. 회차 조회
        ShowTime showTime = showTimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 공연 회차입니다. ID: " + showtimeId
                ));

        // 2. 이 회차에 연결된 ShowSeat 전부 조회 (가능/불가능 모두)
        List<ShowSeat> showSeats = showSeatRepository.findByShowTime(showTime);

        // 3. ShowSeat -> SeatResponseDto 매핑
        List<SeatResponseDto> seats = showSeats.stream()
                .map(showSeat -> {
                    Seat seat = showSeat.getSeat();

                    SeatResponseDto dto = new SeatResponseDto();
                    dto.setShowSeatId(showSeat.getId());
                    dto.setSeatId(seat.getId());
                    dto.setSeatFloor(seat.getFloor());
                    dto.setSeatSection(seat.getSection());
                    dto.setSeatRow(seat.getSeatRow());
                    dto.setSeatColumn(seat.getSeatColumn());
                    dto.setSeatTable(seat.getSeatNumber());
                    dto.setIsAvailable(showSeat.isAvailable());
                    return dto;
                })
                .toList();

        // 4. 상위 DTO 조립
        return new ShowSeatsResponse(
                showTime.getId(),
                showTime.getStartAt(),
                seats
        );
    }

    @Transactional(readOnly = true)
    public void saveSelectedSeats(Long showtimeId,
                                  BookingDto.SessionInfo sessionDto,
                                  java.util.List<Long> showSeatIds) {

        // 1. 회차 검증
        ShowTime showTime = showTimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 공연 회차입니다. ID: " + showtimeId
                ));

        // 2. 선택한 좌석들 조회
        java.util.List<ShowSeat> showSeats = showSeatRepository.findAllById(showSeatIds);

        if (showSeats.size() != showSeatIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일부 좌석 정보를 찾을 수 없습니다.");
        }

        // 3. 모든 좌석이 이 showTime에 속하는지 + 현재 예매 가능인지 체크
        for (ShowSeat ss : showSeats) {
            if (!ss.getShowTime().getId().equals(showTime.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 회차의 좌석이 포함되어 있습니다.");
            }
            if (!ss.isAvailable()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 예매된 좌석이 포함되어 있습니다.");
            }
        }

        // 4. seatTable 리스트 만들기 (Seat.seatNumber 사용)
        java.util.List<String> seatTables = showSeats.stream()
                .map(ss -> ss.getSeat().getSeatNumber()) // 예: "A3-7"
                .toList();

        // 5. 세션 DTO에 저장
        sessionDto.setSelectedShowSeatIds(showSeatIds);
        sessionDto.setSelectedSeatTables(seatTables);
    }
}
