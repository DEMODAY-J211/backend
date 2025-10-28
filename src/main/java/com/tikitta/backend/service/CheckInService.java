package com.tikitta.backend.service;

import com.tikitta.backend.domain.ReservationItem;
import com.tikitta.backend.domain.ShowSeat;
import com.tikitta.backend.dto.QrReadResponseDto;
import com.tikitta.backend.exception.CustomException;
import com.tikitta.backend.exception.ErrorCode;
import com.tikitta.backend.repository.ReservationItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final ReservationItemRepository reservationItemRepository;

    @Transactional
    public QrReadResponseDto checkInWithQrCode(Long showtimeId, String qrCodeContent) {
        // 1. QR 코드 내용으로 ReservationItem 조회
        long reservationItemId = Long.parseLong(qrCodeContent);
        ReservationItem item = reservationItemRepository.findById(reservationItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.QR_NOT_FOUND));

        // 2. Showtime ID 일치 여부 검증
        if (!item.getReservation().getShowTime().getId().equals(showtimeId)) {
            throw new CustomException(ErrorCode.SHOWTIME_MISMATCH);
        }

        // 3. 이미 입장 처리된 티켓인지 검증
        if (item.isEntered()) {
            throw new CustomException(ErrorCode.ALREADY_CHECKED_IN);
        }

        // 4. 입장 처리
        item.checkIn();

        // 5. 좌석제인 경우, 해당 좌석 점유 처리
        ShowSeat showSeat = item.getShowSeat();
        if (showSeat != null) {
            showSeat.reserve(); // isAvailable을 false로 변경
        }

        // 6. 응답 데이터 생성
        boolean isSeatAvailable = (showSeat != null) ? showSeat.isAvailable() : true; // 스탠딩은 항상 true
        return QrReadResponseDto.from(item, isSeatAvailable);
    }
}
