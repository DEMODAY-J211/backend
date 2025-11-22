package com.tikitta.backend.controller;

import com.tikitta.backend.domain.Message;
import com.tikitta.backend.domain.Reservation;
import com.tikitta.backend.repository.MessageRepository;
import com.tikitta.backend.repository.ReservationRepository;
import com.tikitta.backend.util.SmsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final SmsUtil smsUtil;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;

    /**
     * SMS 발송 기능을 테스트하기 위한 임시 API
     */
    @GetMapping("/sms")
    public ResponseEntity<?> sendTestSms(
            @RequestParam("phone") String phone,
            @RequestParam("message") String message
    ) {
        try {
            Map<String, String> result = smsUtil.sendSms(phone, message);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * LMS 발송 및 메시지 포맷팅을 테스트하기 위한 임시 API
     * Reservation ID 1L을 기준으로 입금 안내 메시지를 발송합니다.
     */
    @GetMapping("/lms")
    public ResponseEntity<?> sendTestLms(
            @RequestParam("phone") String phone
    ) {
        try {
            // 1. DB에서 테스트용 데이터 조회
            Reservation reservation = reservationRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("테스트할 예매 데이터(ID: 1)가 DB에 없습니다."));
            
            Message messageTemplate = messageRepository.findByShow(reservation.getShowTime().getShow())
                    .orElseThrow(() -> new RuntimeException("해당 공연의 메시지 템플릿이 DB에 없습니다."));

            // 2. 입금 안내 템플릿 선택
            String paymentGuideTemplate = messageTemplate.getPaymentGuide();
            if (paymentGuideTemplate == null || paymentGuideTemplate.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "입금 안내 메시지 템플릿이 비어있습니다."));
            }

            // 3. 메시지 포맷팅
            String finalMessage = SmsUtil.formatMessage(paymentGuideTemplate, reservation);
            String subject = String.format("[%s] 입금 안내", reservation.getShowTime().getShow().getTitle());

            // 4. LMS 발송
            Map<String, String> result = smsUtil.sendLms(phone, subject, finalMessage);
            
            // 5. 결과와 함께 가공된 메시지 내용도 함께 반환
            return ResponseEntity.ok(Map.of(
                "gabia_response", result,
                "sent_subject", subject,
                "sent_message", finalMessage
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
