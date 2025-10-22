package com.tikitta.backend.service;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.BookingDto;
import com.tikitta.backend.dto.BookingInfoResponse;
import com.tikitta.backend.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            return showSeatRepository.countByShowTimeAndIsAvailable(showTime, true);        }

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
    public int calculateTotalPrice(Long ticketOptionId, Integer quantity){
        TicketOption ticketOption = ticketOptionRepository.findById(ticketOptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 옵션입니다."));
        return ticketOption.getPrice() * quantity;
    }

    /**
     * 예매 최종 확정 (Reservation 및 ReservationItem 생성)
     */
    @Transactional
    public Reservation createReservation(BookingDto.SessionInfo sessionDto,
                                         Authentication authentication) {
        // 1. 로그인된 사용자 정보 조회
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");
        KakaoOauth user = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 사용자 정보를 찾을 수 없습니다."));

        // 2. 예매하려는 회차 정보 조회
        ShowTime showTime = showTimeRepository.findById(sessionDto.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 회차입니다."));
        Shows show = showTime.getShow();

        // 3. Reservation 엔티티 생성 및 저장
        Reservation reservation = Reservation.builder()
                .user(user)
                .showTime(showTime)
                .quantity(sessionDto.getQuantity())
                .totalPrice(sessionDto.getCalculatedTotalPrice())
                .refundAccountNumber(sessionDto.getRefundBank() + " " + sessionDto.getRefundAccount() + " " + sessionDto.getRefundHolder()) // 환불 정보 조합
                .status(DomainEnums.ReservationStatus.PENDING_PAYMENT) // ◀ 입금 대기 상태
                .createdAt(LocalDateTime.now())
                .build();
        reservationRepository.save(reservation);

        // --- 4. ReservationItem 생성 (스탠딩 기준) ---
        // TODO: 좌석제일 경우, sessionDto에서 selectedShowSeatIds를 꺼내 반복문으로 ShowSeat 상태 변경 및 Item 생성 필요

        List<ReservationItem> items = new ArrayList<>();
        // 4-1. 공연 유형 확인
        DomainEnums.SaleMethod saleMethod = show.getSaleMethod();
        if (saleMethod == DomainEnums.SaleMethod.Select_by_User) {
            // --- 4-2. 좌석제 ---
            // TODO: sessionDto에서 selectedShowSeatIds를 꺼내 반복문으로 ShowSeat 상태 변경 및 Item 생성
            // List<Long> seatIds = sessionDto.getSelectedShowSeatIds();
            // for (Long seatId : seatIds) {
            //     ShowSeat showSeat = showSeatRepository.findById(seatId).orElseThrow();
            //     showSeat.reserve(); // 좌석 점유 처리
            //     ReservationItem item = ReservationItem.builder()
            //             .reservation(reservation)
            //             .showSeat(showSeat)
            //             .build();
            //     items.add(item);
            // }

        } else  {
            // --- 4-3. 스탠딩 ---
            // 4-3-1. 현재 회차의 마지막 입장 번호 조회
            Integer maxEntryNumber = reservationItemRepository.findMaxEntryNumberByShowTime(showTime);
            int nextEntryNumber = (maxEntryNumber != null) ? maxEntryNumber + 1 : 1; // 첫 예매면 1번부터 시작

            // 4-3-2. 예매 매수만큼 순차적으로 입장 번호 부여
            for (int i = 0; i < sessionDto.getQuantity(); i++) {
                ReservationItem item = ReservationItem.builder()
                        .reservation(reservation)
                        // .showSeat(null) // 스탠딩은 null
                        .entryNumber(nextEntryNumber + i) // ◀ 입장 번호 할당
                        // .qrCodeUrl(...) // QR 코드 생성 로직 필요
                        .build();
                items.add(item);
            }
        }

        reservationItemRepository.saveAll(items); // 생성된 Item들 저장

        return reservation; // 생성된 예매 정보 반환 (ID 등 확인용)
    }
}
