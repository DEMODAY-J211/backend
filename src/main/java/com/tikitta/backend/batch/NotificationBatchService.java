package com.tikitta.backend.batch;

import com.tikitta.backend.domain.Message;
import com.tikitta.backend.domain.Reservation;
import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.repository.MessageRepository;
import com.tikitta.backend.repository.ReservationRepository;
import com.tikitta.backend.repository.ShowTimeRepository;
import com.tikitta.backend.util.SmsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationBatchService {

    private final ShowTimeRepository showTimeRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final SmsUtil smsUtil;

    @Value("${frontend-url}")
    private String frontendUrl;

    /**
     * 매 1분마다 실행되어, 2시간 뒤에 시작하는 공연의 안내 메시지를 발송합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void sendShowReminders() {
        log.info("공연 시작 전 안내 메시지 배치 작업 시작: {}", LocalDateTime.now());

        // 1. "지금으로부터 2시간 뒤 ~ 1시간 59분 뒤" 사이에 시작하는 공연 회차 조회
        LocalDateTime from = LocalDateTime.now().plusHours(1);
        LocalDateTime to = from.plusMinutes(1);

        List<ShowTime> targetShowTimes = showTimeRepository.findAllByStartAtBetween(from, to);

        if (targetShowTimes.isEmpty()) {
            log.info("1시간 내 시작하는 공연과 끝난 공연이 없어 배치 작업을 종료합니다.");
            return;
        }

        for (ShowTime showTime : targetShowTimes) {
            log.info("공연 회차 [{}]의 안내 메시지 발송을 시작합니다.", showTime.getId());

            // 2. 해당 공연의 메시지 템플릿 조회
            Optional<Message> messageOpt = messageRepository.findByShow(showTime.getShow());
            if (messageOpt.isEmpty() || !StringUtils.hasText(messageOpt.get().getShowGuide())) {
                log.warn("공연 안내 메시지 템플릿이 없어, 공연 회차 [{}]의 메시지 발송을 건너뜁니다.", showTime.getId());
                continue;
            }
            String template = messageOpt.get().getShowGuide();

            // 3. 해당 회차의 모든 예매 건 조회
            List<Reservation> reservations = reservationRepository.findAllByShowTime(showTime);

            for (Reservation reservation : reservations) {
                try {
                    // 4. 메시지 가공
                    String formattedMessage = SmsUtil.formatMessage(template, reservation);
                    
                    // 5. QR코드 URL 추가
                    Long managerId = reservation.getShowTime().getShow().getManager().getId();
                    Long reservationId = reservation.getId();
                    String qrUrl = String.format("%s/%d/mobileticket/%d", frontendUrl, managerId, reservationId);
                    
                    String finalMessage = formattedMessage + "\n\n" + "입장 QR코드 확인: " + qrUrl;
                    String subject = "[티킷타] 공연 안내";

                    // 6. LMS 발송
                    smsUtil.sendLms(reservation.getPhone(), subject, finalMessage);
                    log.info("공연 안내 메시지 발송 완료: Reservation ID {}", reservation.getId());

                } catch (Exception e) {
                    log.error("공연 안내 메시지 발송 중 오류 발생. Reservation ID: {}", reservation.getId(), e);
                }
            }
        }
        log.info("공연 시작 전 안내 메시지 배치 작업 종료: {}", LocalDateTime.now());
    }

    /**
     * 매 1분마다 실행되어, 1시간 전에 종료된 공연의 리뷰 요청 메시지를 발송합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void sendReviewRequests() {
        log.info("공연 후 리뷰 요청 메시지 배치 작업 시작: {}", LocalDateTime.now());

        // 1. "지금으로부터 1시간 전 ~ 1시간 1분 전" 사이에 종료된 공연 회차 조회
        LocalDateTime from = LocalDateTime.now().minusHours(1).minusMinutes(1);
        LocalDateTime to = LocalDateTime.now().minusHours(1);

        List<ShowTime> targetShowTimes = showTimeRepository.findAllByEndAtBetween(from, to);

        if (targetShowTimes.isEmpty()) {
            return;
        }

        for (ShowTime showTime : targetShowTimes) {
            log.info("공연 회차 [{}]의 리뷰 요청 메시지 발송을 시작합니다.", showTime.getId());

            // 2. 해당 공연의 메시지 템플릿 및 리뷰 URL 조회
            Optional<Message> messageOpt = messageRepository.findByShow(showTime.getShow());
            String reviewUrl = showTime.getShow().getReviewUrl();

            if (messageOpt.isEmpty() || !StringUtils.hasText(messageOpt.get().getReviewRequest()) || !StringUtils.hasText(reviewUrl)) {
                log.warn("리뷰 요청 템플릿 또는 리뷰 URL이 없어, 공연 회차 [{}]의 메시지 발송을 건너뜁니다.", showTime.getId());
                continue;
            }
            String template = messageOpt.get().getReviewRequest();

            // 3. 해당 회차의 모든 예매 건 조회
            List<Reservation> reservations = reservationRepository.findAllByShowTime(showTime);

            for (Reservation reservation : reservations) {
                try {
                    // 4. 메시지 가공
                    String formattedMessage = SmsUtil.formatMessage(template, reservation);
                    String finalMessage = formattedMessage + "\n\n" + "리뷰 작성하기: " + reviewUrl;
                    String subject = "[티킷타] 공연 후기 안내";

                    // 5. LMS 발송
                    smsUtil.sendLms(reservation.getPhone(), subject, finalMessage);
                    log.info("리뷰 요청 메시지 발송 완료: Reservation ID {}", reservation.getId());

                } catch (Exception e) {
                    log.error("리뷰 요청 메시지 발송 중 오류 발생. Reservation ID: {}", reservation.getId(), e);
                }
            }
        }
        log.info("공연 후 리뷰 요청 메시지 배치 작업 종료: {}", LocalDateTime.now());
    }
}
