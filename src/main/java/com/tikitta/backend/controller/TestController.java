package com.tikitta.backend.controller;

import com.tikitta.backend.util.SmsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final SmsUtil smsUtil;

    /**
     * SMS 발송 기능을 테스트하기 위한 임시 API
     * @param phone 수신할 휴대폰 번호
     * @param message 보낼 메시지 내용
     * @return Gabia API로부터 받은 응답 결과
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
            // 에러 발생 시, 에러 메시지를 응답으로 보냅니다.
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}