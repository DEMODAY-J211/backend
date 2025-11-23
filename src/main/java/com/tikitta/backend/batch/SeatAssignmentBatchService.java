package com.tikitta.backend.batch;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.repository.ReservationItemRepository;
import com.tikitta.backend.repository.ReservationRepository;
import com.tikitta.backend.repository.ShowSeatRepository;
import com.tikitta.backend.repository.ShowTimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatAssignmentBatchService {

    private final ShowTimeRepository showTimeRepository;
    private final ReservationRepository reservationRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ReservationItemRepository reservationItemRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void assignSeatsForScheduledShows() {
        log.info("좌석 자동 배정 배치 작업 시작: {}", LocalDateTime.now());

        LocalDateTime from = LocalDateTime.now().plusHours(1).plusMinutes(1);
        LocalDateTime to = from.plusMinutes(1);

        List<ShowTime> targetShowTimes = showTimeRepository.findShowsToAssignSeats(
                DomainEnums.SaleMethod.SCHEDULING, from, to
        );

        if (targetShowTimes.isEmpty()) {
            return;
        }

        for (ShowTime showTime : targetShowTimes) {
            log.info("공연 회차 [{}]의 좌석 배정을 시작합니다.", showTime.getId());
            try {
                processSeatAssignment(showTime);
                log.info("공연 회차 [{}]의 좌석 배정을 성공적으로 완료했습니다.", showTime.getId());
            } catch (Exception e) {
                log.error("공연 회차 [{}]의 좌석 배정 중 오류가 발생했습니다.", showTime.getId(), e);
            }
        }
        log.info("좌석 자동 배정 배치 작업 종료: {}", LocalDateTime.now());
    }

    private void processSeatAssignment(ShowTime showTime) {
        // --- 1. 데이터 준비 ---
        // 1-1. 사용 가능한 모든 좌석 조회 및 정렬
        List<ShowSeat> allAvailableSeats = showSeatRepository.findByShowTime(showTime).stream()
                .filter(ShowSeat::isAvailable)
                .sorted(Comparator.comparing((ShowSeat ss) -> ss.getSeat().getSeatRow())
                        .thenComparing(ss -> ss.getSeat().getSeatColumn()))
                .collect(Collectors.toList());

        // 1-2. VIP석과 일반석으로 그룹화
        List<ShowSeat> vipSeats = allAvailableSeats.stream()
                .filter(ss -> ss.getIsGoodSeat() != null && ss.getIsGoodSeat())
                .collect(Collectors.toList());
        List<ShowSeat> normalSeats = allAvailableSeats.stream()
                .filter(ss -> ss.getIsGoodSeat() == null || !ss.getIsGoodSeat())
                .collect(Collectors.toList());

        // 1-3. 예매 목록을 예매순으로 정렬하여 조회
        List<Reservation> reservations = reservationRepository.findByShowTimeOrderByCreatedAtAsc(showTime);

        List<ReservationItem> updatedItems = new ArrayList<>();

        // --- 2. 자동 배정 알고리즘 실행 ---
        for (Reservation reservation : reservations) {
            int quantity = reservation.getQuantity();
            List<ReservationItem> items = reservation.getReservationItems();

            // 이미 좌석이 배정된 예매는 건너뜀
            if (items.stream().anyMatch(item -> item.getShowSeat() != null)) {
                continue;
            }

            // 2-1. 연속된 빈 좌석 찾기
            List<ShowSeat> assignedSeats = findConsecutiveSeats(vipSeats, quantity);
            if (assignedSeats.isEmpty()) {
                assignedSeats = findConsecutiveSeats(normalSeats, quantity);
            }

            // 2-2. 연속 좌석이 없으면, 남은 좌석에서 최선 노력으로 배정
            if (assignedSeats.isEmpty()) {
                assignedSeats = findBestEffortSeats(vipSeats, quantity);
                if (assignedSeats.size() < quantity) {
                    assignedSeats.addAll(findBestEffortSeats(normalSeats, quantity - assignedSeats.size()));
                }
            }

            // --- 3. 결과 저장 ---
            if (assignedSeats.size() == quantity) {
                for (int i = 0; i < quantity; i++) {
                    ReservationItem item = items.get(i);
                    ShowSeat seat = assignedSeats.get(i);
                    item.setShowSeat(seat); // ReservationItem에 ShowSeat 연결
                    seat.reserve(); // ShowSeat을 '사용 중'으로 상태 변경
                    updatedItems.add(item);
                }
            } else {
                log.warn("Reservation ID [{}]: 요청 수량({})만큼 좌석을 배정하지 못했습니다. (배정된 수: {})",
                        reservation.getId(), quantity, assignedSeats.size());
            }
        }

        if (!updatedItems.isEmpty()) {
            reservationItemRepository.saveAll(updatedItems);
        }
    }

    private List<ShowSeat> findConsecutiveSeats(List<ShowSeat> seatList, int quantity) {
        for (int i = 0; i <= seatList.size() - quantity; i++) {
            boolean isConsecutive = true;
            ShowSeat firstSeat = seatList.get(i);

            // 같은 행에 있는지, 열이 연속되는지 확인
            for (int j = 1; j < quantity; j++) {
                ShowSeat currentSeat = seatList.get(i + j);
                ShowSeat prevSeat = seatList.get(i + j - 1);
                if (!currentSeat.getSeat().getSeatRow().equals(prevSeat.getSeat().getSeatRow()) ||
                    currentSeat.getSeat().getSeatColumn() != prevSeat.getSeat().getSeatColumn() + 1) {
                    isConsecutive = false;
                    break;
                }
            }

            if (isConsecutive) {
                return new ArrayList<>(seatList.subList(i, i + quantity));
            }
        }
        return new ArrayList<>(); // 연속된 좌석을 찾지 못함
    }

    private List<ShowSeat> findBestEffortSeats(List<ShowSeat> seatList, int quantity) {
        List<ShowSeat> assigned = new ArrayList<>();
        int count = 0;
        while (count < quantity && !seatList.isEmpty()) {
            assigned.add(seatList.remove(0));
            count++;
        }
        return assigned;
    }
}
