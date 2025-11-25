package com.tikitta.backend.service;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.userbooking.*;
import com.tikitta.backend.repository.*;
import com.tikitta.backend.util.SmsUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
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
    private final QrCodeService qrCodeService;
    private final MessageRepository messageRepository;
    private final SmsUtil smsUtil;
    private final ImageService imageService;

    public BookingInfoResponse getBookingInfo(Long showId) {
        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다. ID: " + showId));

        List<BookingInfoResponse.ShowTimeItemDto> showTimeDtos =
                show.getShowTimes().stream()
                        .map(showTime -> {
                            int availableSeats = calculateAvailableSeats(showTime);
                            return new BookingInfoResponse.ShowTimeItemDto(showTime, availableSeats);
                        })
                        .collect(Collectors.toList());

        return new BookingInfoResponse(show, showTimeDtos);
    }

    private int calculateAvailableSeats(ShowTime showTime) {
        Shows show = showTime.getShow();
        DomainEnums.SaleMethod saleMethod = show.getSaleMethod();
        List<DomainEnums.ReservationStatus> activeStatuses = List.of(
                DomainEnums.ReservationStatus.CONFIRMED,
                DomainEnums.ReservationStatus.PENDING_PAYMENT
        );

        // 1. 좌석제-직접선택 공연일 경우 (새로운 모델 적용)
        if (saleMethod == DomainEnums.SaleMethod.SELECTBYUSER) {
            return showSeatRepository.countByShowTimeAndIsAvailable(showTime, true);
        }

        /*TODO: long int 문제 발생*/
        // 2. 그 외 (스탠딩, 스케줄링, 주최자선택) 공연일 경우 (수량 계산)
        else {
            Long totalQuantity = showTime.getTotalStandingQuantity();
            if (totalQuantity == null || totalQuantity <= 0) {
                return 0;
            }
            List<Reservation> reservations = reservationRepository.findByShowTimeAndStatusIn(showTime, activeStatuses);
            Long bookedQuantity = (long) reservations.stream()
                    .mapToInt(Reservation::getQuantity)
                    .sum();
            return Math.toIntExact(Math.max(0L, totalQuantity - bookedQuantity)); // 음수 방지
        }

    }

    //예매 총 가격 계산
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
    public Reservation createReservation(Long managerId, BookingDto.SessionInfo sessionDto,
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

        // 확인
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
        String newReservationNumber = generateReservationNumber(show.getSaleMethod(), user);


        // --- Reservation 저장 ---
        Reservation reservation = Reservation.builder()
                .user(user)
                .reservationNumber(newReservationNumber)
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

        DomainEnums.SaleMethod saleMethod = show.getSaleMethod();
        if (saleMethod == DomainEnums.SaleMethod.SELECTBYUSER) {
            // ========== 좌석제 ==========
            List<Long> showSeatIds = sessionDto.getSelectedShowSeatIds();
            if (showSeatIds == null || showSeatIds.isEmpty()) {
                throw new IllegalStateException("좌석제 공연에서는 좌석 선택 후 예매를 진행해야 합니다.");
            }

            List<ShowSeat> showSeats = showSeatRepository.findAllById(showSeatIds);
            if (showSeats.size() != showSeatIds.size()) {
                throw new IllegalStateException("일부 좌석 정보를 찾을 수 없습니다.");
            }

            for (ShowSeat ss : showSeats) {
                if (!ss.getShowTime().getId().equals(showTime.getId())) {
                    throw new IllegalStateException("다른 회차의 좌석 포함됨");
                }
                if (!ss.isAvailable()) {
                    throw new IllegalStateException("이미 예약된 좌석 포함됨");
                }

                ss.reserve();
                showSeatRepository.save(ss);

                items.add(ReservationItem.builder()
                        .reservation(reservation)
                        .showSeat(ss)
                        .status(DomainEnums.ReservationStatus.PENDING_PAYMENT)
                        .build());
            }

        } else if (saleMethod == DomainEnums.SaleMethod.SCHEDULING) {
            // ========== 스케줄링 ==========
            for (int i = 0; i < sessionDto.getQuantity(); i++) {
                items.add(ReservationItem.builder()
                    .reservation(reservation)
                    .status(DomainEnums.ReservationStatus.PENDING_PAYMENT)
                    .build());
            }

        } else {
            // ========== 스탠딩, 주최자 지정 ==========
            Integer maxEntry = reservationItemRepository.findMaxEntryNumberByShowTime(showTime);
            int nextEntry = (maxEntry != null) ? maxEntry + 1 : 1;

            for (int i = 0; i < sessionDto.getQuantity(); i++) {
                items.add(ReservationItem.builder()
                        .reservation(reservation)
                        .entryNumber(nextEntry + i)
                        .status(DomainEnums.ReservationStatus.PENDING_PAYMENT)
                        .build());
            }
        }

        reservationItemRepository.saveAll(items);


        // QR-code 생성
        for (ReservationItem item : items) {
            if (item.getId() == null) {
                log.warn("ReservationItem ID가 null입니다. QR 코드를 생성할 수 없습니다.");
                continue;
            }

            try {
                Shows Qrshow = showTime.getShow();
                Long showId = Qrshow.getId();
                String qrContent = String.valueOf(item.getId());
                String qrUrl = qrCodeService.createAndUploadQrCode(managerId, showId, qrContent);
                item.setQrCodeUrl(qrUrl);
            } catch (Exception e) {
                log.error("ReservationItem ID [{}]에 대한 QR 코드 생성 실패", item.getId(), e);
                throw new RuntimeException("QR 코드 생성에 실패했습니다.", e);
            }
        }

        reservationItemRepository.saveAll(items);

        return reservation;
    }

    /**
     * 예매 번호 생성 헬퍼 메소드 (알파벳2 + yymmddHHmm + userId)
     */
    private String generateReservationNumber(DomainEnums.SaleMethod saleMethod, KakaoOauth user) {
        String prefix = switch (saleMethod) {
            case EVENTHOST -> "EH";
            case SCHEDULING -> "SD";
            case STANDING -> "ST";
            case SELECTBYUSER -> "US";
            default -> "XX";
        };

        String dateTimePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"));

        String userIdPart = String.valueOf(user.getId());

        return prefix + dateTimePart + userIdPart; // 예: "US2510222015" + "1" -> "US25102220151"
    }

    public ReservationDetailResponse getReservationDetail(Long reservationId, Authentication authentication) {

        Reservation reservation = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다. ID: " + reservationId));

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        if (!reservation.getUser().getEmail().equals(email)) {
            throw new SecurityException("자신의 예매 내역만 조회할 수 있습니다.");
        }


        return new ReservationDetailResponse(reservation);
    }

    @Transactional
    public void cancelReservation(Long reservationId, Authentication authentication) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 예매입니다. ID: " + reservationId));

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        if (!reservation.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("자신의 예매 내역만 취소할 수 있습니다.");
        }

        boolean success = reservation.requestCancellation();

        if (!success) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 취소되었거나 취소할 수 없는 예매입니다.");
        }

        log.info("예매 취소 요청 완료: Reservation ID={}, New Status={}", reservationId, reservation.getStatus());

        reservation.getReservationItems().forEach(item -> {
            item.setStatus(DomainEnums.ReservationStatus.CANCEL_REQUESTED);
        });

        // --- 좌석제 공연 (Select_by_User) ---
        if (reservation.getShowTime().getShow().getSaleMethod() == DomainEnums.SaleMethod.SELECTBYUSER) {
            reservation.getReservationItems().forEach(item -> {
                ShowSeat ss = item.getShowSeat();
                if (ss != null) {
                    ss.cancel();
                }
            });
        }

        // --- QR 코드 이미지 S3에서 삭제 ---
        reservation.getReservationItems().forEach(item -> {
            String qrUrl = item.getQrCodeUrl();
            if (qrUrl != null && !qrUrl.isBlank()) {
                imageService.delete(qrUrl);   // S3에서 삭제
                item.setQrCodeUrl(null);      // DB에서도 URL 제거 (setter 필요)
            }
        });
    }

    @Transactional(readOnly = true)
    public ShowSeatsResponse getShowSeats(Long showtimeId) {

        ShowTime showTime = showTimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 공연 회차입니다. ID: " + showtimeId
                ));

        List<ShowSeat> showSeats = showSeatRepository.findByShowTime(showTime);

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

        ShowTime showTime = showTimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 공연 회차입니다. ID: " + showtimeId
                ));

        java.util.List<ShowSeat> showSeats = showSeatRepository.findAllById(showSeatIds);

        if (showSeats.size() != showSeatIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일부 좌석 정보를 찾을 수 없습니다.");
        }

        for (ShowSeat ss : showSeats) {
            if (!ss.getShowTime().getId().equals(showTime.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 회차의 좌석이 포함되어 있습니다.");
            }
            if (!ss.isAvailable()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 예매된 좌석이 포함되어 있습니다.");
            }
        }

        java.util.List<String> seatTables = showSeats.stream()
                .map(ss -> ss.getSeat().getSeatNumber()) // 예: "A3-7"
                .toList();

        sessionDto.setSelectedShowSeatIds(showSeatIds);
        sessionDto.setSelectedSeatTables(seatTables);
    }

    @Transactional
    public void changeSeats(Long reservationId,
                            List<Long> newShowSeatIds,
                            Authentication authentication) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 예매입니다. ID: " + reservationId
                ));

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        if (!reservation.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("자신의 예매에 대해서만 좌석을 변경할 수 있습니다.");
        }

        Shows show = reservation.getShowTime().getShow();
        if (show.getSaleMethod() != DomainEnums.SaleMethod.SELECTBYUSER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "좌석제 공연에서만 좌석 변경이 가능합니다.");
        }

        if (newShowSeatIds == null || newShowSeatIds.size() != reservation.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 좌석 수가 예매 수량과 일치하지 않습니다.");
        }

        ShowTime showTime = reservation.getShowTime();

        List<ShowSeat> newShowSeats = showSeatRepository.findAllById(newShowSeatIds);
        if (newShowSeats.size() != newShowSeatIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 좌석이 포함되어 있습니다.");
        }

        for (ShowSeat ss : newShowSeats) {

            if (!ss.getShowTime().getId().equals(showTime.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "선택한 좌석 중 이 예매의 회차와 다른 좌석이 있습니다.");
            }

            boolean isCurrentlyMine = reservation.getReservationItems().stream()
                    .anyMatch(item -> item.getShowSeat() != null
                            && item.getShowSeat().getId().equals(ss.getId()));

            if (!isCurrentlyMine && !ss.isAvailable()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "이미 예약된 좌석이 포함되어 있습니다.");
            }
        }

        for (ReservationItem item : reservation.getReservationItems()) {
            ShowSeat oldSeat = item.getShowSeat();
            if (oldSeat != null) {
                oldSeat.cancel();         // isAvailable = true
                showSeatRepository.save(oldSeat);
            }
        }

        List<ReservationItem> items = reservation.getReservationItems();
        if (items.size() != newShowSeats.size()) {
            throw new IllegalStateException("ReservationItem 수와 예매 수량이 일치하지 않습니다.");
        }

        for (int i = 0; i < items.size(); i++) {
            ReservationItem item = items.get(i);
            ShowSeat newSeat = newShowSeats.get(i);

            newSeat.reserve();            // isAvailable = false
            showSeatRepository.save(newSeat);

            item.setShowSeat(newSeat);
        }

        log.info("좌석 변경 완료: reservationId={}, newSeatIds={}", reservationId, newShowSeatIds);
    }

    @Transactional
    public void sendPaymentGuide(Long reservationId, Authentication authentication) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 예매입니다. ID: " + reservationId
                ));

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        if (!reservation.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("자신의 예매에 대해서만 입금 안내를 받을 수 있습니다.");
        }
        Shows show = reservation.getShowTime().getShow();
        Message message = messageRepository.findByShow(show).orElse(null);

        if (message == null) return;

        String paymentGuideTemplate = message.getPaymentGuide();

        if (paymentGuideTemplate != null && !paymentGuideTemplate.isBlank()) {
            try {
                String finalMessage = SmsUtil.formatMessage(paymentGuideTemplate, reservation);
                smsUtil.sendLms(
                        reservation.getUserPhone(),
                        "[티킷타] 입금 안내",
                        finalMessage
                );
            } catch (Exception e) {
                log.error("입금 안내 LMS 발송 실패. reservationId={}", reservation.getId(), e);
            }
        }
    }
}
