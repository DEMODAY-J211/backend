package com.tikitta.backend.controller;

import com.tikitta.backend.dto.ApiResponse;
import com.tikitta.backend.dto.BookingInfoResponse;
import com.tikitta.backend.service.UserBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/{managerId}/booking")
public class UserBookingController {

    private final UserBookingService userBookingService;

    @GetMapping("/{showId}/reserveInfo") // ◀ 2. 엔드포인트 추가
    public ResponseEntity<ApiResponse<BookingInfoResponse>> getReserveInfo(
            @PathVariable Long managerId,
            @PathVariable Long showId) {

        // 3. Service 호출 및 ApiResponse로 감싸서 반환
        BookingInfoResponse data = userBookingService.getBookingInfo(showId);
        return ResponseEntity.ok(new ApiResponse<>(data));
    }
}
