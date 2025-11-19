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
     * LMS 발송 기능을 테스트하기 위한 임시 API
     */
    @GetMapping("/lms")
    public ResponseEntity<?> sendTestLms(
            @RequestParam("phone") String phone,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message
    ) {
        try {
            Map<String, String> result = smsUtil.sendLms(phone, subject, message);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}