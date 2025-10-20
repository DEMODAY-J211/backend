package com.tikitta.backend.service;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.BookingInfoResponse;
import com.tikitta.backend.repository.ReservationRepository;
import com.tikitta.backend.repository.ShowSeatRepository;
import com.tikitta.backend.repository.ShowsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserBookingService {

    private final ShowsRepository showsRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ReservationRepository reservationRepository;

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
        DomainEnums.LocationType type = show.getLocation().getType();

        // 예매 완료/대기중인 상태 목록
        List<DomainEnums.ReservationStatus> activeStatuses = List.of(
                DomainEnums.ReservationStatus.CONFIRMED,
                DomainEnums.ReservationStatus.PENDING_PAYMENT
        );

        // 1. 좌석제 공연일 경우 (새로운 모델 적용)
        if (type == DomainEnums.LocationType.SEATED) {
            // "이 회차"에 할당된 좌석 중 "isAvailable = true"인 좌석 수
            return showSeatRepository.countByShowTimeAndIsAvailable(showTime, true);
        }

        // 2. 스탠딩 공연일 경우 (수량 계산)
        else if (type == DomainEnums.LocationType.STANDING) {
            // 2-1. 이 공연의 총 티켓 수량 (모든 회차가 공유한다고 가정)
            int totalQuantity = show.getTicketOptions().stream()
                    .mapToInt(TicketOption::getQuantity)
                    .sum();

            // 2-2. "이 회차"에 예매된 티켓 수량 (Reservation의 quantity)
            List<Reservation> reservations = reservationRepository.findByShowTimeAndStatusIn(showTime, activeStatuses);
            int bookedQuantity = reservations.stream()
                    .mapToInt(Reservation::getQuantity)
                    .sum();

            return totalQuantity - bookedQuantity;
        }

        return 0; // 그 외의 경우
    }
}
