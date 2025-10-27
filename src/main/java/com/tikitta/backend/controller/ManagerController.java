package com.tikitta.backend.controller;

import com.tikitta.backend.dto.QrReadResponseDto;
import com.tikitta.backend.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/manager/shows")
public class ManagerController {

    private final CheckInService checkInService;

    @GetMapping("/{showId}/QR")
    public ResponseEntity<Object> checkInByQrCode(
            @PathVariable Long showId, // 경로 변수는 현재 로직에서 사용되지 않지만, 명세에 따라 유지
            @RequestParam("showtimeId") Long showtimeId,
            @RequestParam("code") String qrCode) {

        QrReadResponseDto responseDto = checkInService.checkInWithQrCode(showtimeId, qrCode);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", 200,
                "message", "success:입장 완료",
                "data", responseDto
        ));
    }
}
