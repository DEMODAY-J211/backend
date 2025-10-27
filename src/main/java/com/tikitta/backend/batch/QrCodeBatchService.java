package com.tikitta.backend.batch;

import com.tikitta.backend.domain.Reservation;
import com.tikitta.backend.domain.ReservationItem;
import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.repository.ReservationRepository;
import com.tikitta.backend.repository.ShowTimeRepository;
import com.tikitta.backend.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QrCodeBatchService {

    private final ShowTimeRepository showTimeRepository;
    private final ReservationRepository reservationRepository;
    private final QrCodeService qrCodeService;

    /**
     * 매 10분마다 실행되어, 2시간 뒤에 시작하는 공연의 QR코드를 생성합니다.
     * cron = "초 분 시 일 월 요일"
     * "0 */10 * * * *" = 매 10분마다 0초에 실행
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void generateQrCodes() {
        log.info("QR 코드 생성 배치 작업 시작: {}", LocalDateTime.now());

        // 1. 공연 시작 2시간 전 ~ 1시간 50분 전 사이의 공연 회차를 조회
        LocalDateTime from = LocalDateTime.now().plusHours(2);
        LocalDateTime to = from.plusMinutes(10);

        List<ShowTime> targetShowTimes = showTimeRepository.findAllByStartAtBetween(from, to);

        for (ShowTime showTime : targetShowTimes) {
            log.info("공연 회차 [{}]의 QR 코드 생성을 시작합니다.", showTime.getId());
            List<Reservation> reservations = reservationRepository.findAllByShowTime(showTime);

            for (Reservation reservation : reservations) {
                for (ReservationItem item : reservation.getReservationItems()) {
                    // QR코드가 아직 생성되지 않은 경우에만 생성
                    if (item.getQrCodeUrl() == null || item.getQrCodeUrl().isEmpty()) {
                        try {
                            String qrContent = String.valueOf(item.getId());
                            // QR코드 생성 및 업로드 후 URL 반환 (QrCodeService에서 구현 필요)
                            String qrCodeUrl = qrCodeService.createAndUploadQrCode(qrContent);

                            item.setQrCodeUrl(qrCodeUrl); // 엔티티에 URL 설정
                            log.info("ReservationItem ID [{}]: QR코드 생성 완료, URL: {}", item.getId(), qrCodeUrl);

                        } catch (Exception e) {
                            log.error("QR 코드 생성 중 오류 발생 (ReservationItem ID: {})", item.getId(), e);
                        }
                    }
                }
            }
        }
        log.info("QR 코드 생성 배치 작업 종료: {}", LocalDateTime.now());
    }
}
