package com.tikitta.backend.controller;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.ApiResponse;
import com.tikitta.backend.dto.CustomerListResponseDto;
import com.tikitta.backend.dto.MyShowListResponseDto;
import com.tikitta.backend.dto.QrReadResponseDto;
import com.tikitta.backend.dto.ReservationStatusUpdateRequest;
import com.tikitta.backend.dto.ReservationStatusUpdateResponse;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.service.CheckInService;
import com.tikitta.backend.service.ShowService;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final KakaoOauthRepository kakaoOauthRepository;
    private final ManagerRepository managerRepository;
    private final ShowService showService;
    private final CheckInService checkInService;


    @GetMapping("/link")
    public ResponseEntity<String> getManagerLink(Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");
        KakaoOauth kakaoOauth = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 Oauth 정보를 찾을 수 없습니다."));
        Manager manager = managerRepository.findByKakaoOauth(kakaoOauth)
                .orElseThrow(() -> new RuntimeException("매니저 정보를 찾을 수 없습니다. (매니저 회원가입이 완료되지 않았을 수 있습니다)"));
        String managerLink = "user/" + manager.getId() + "/main";
        return ResponseEntity.ok(managerLink);
    }

    @GetMapping("/shows/list")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<MyShowListResponseDto>> getMyShows() {
        MyShowListResponseDto myShowList = showService.getMyShows();
        return ResponseEntity.ok(new ApiResponse<>(myShowList));
    }

    @GetMapping("/shows/{showId}/customers")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<CustomerListResponseDto>> getShowCustomers(
            @PathVariable Long showId,
            @RequestParam(required = false) Long showtimeId) {
        CustomerListResponseDto reservationList = showService.getReservationList(showId, showtimeId);
        return ResponseEntity.ok(new ApiResponse<>(reservationList));
    }

    @PatchMapping("/shows/{showId}/customers")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> updateReservationStatus(
        @PathVariable Long showId,
        @RequestParam Long showtimeId,
        @RequestBody ReservationStatusUpdateRequest request) {
        ReservationStatusUpdateResponse responseData = showService.updateReservationStatus(showId,
            showtimeId, request);

        HttpStatus status;
        String message;
        int code;

        if (responseData.getFailedIds() == null || responseData.getFailedIds().isEmpty()) {
            status = HttpStatus.OK;
            code = 200;
            message = "Reservation statuses updated successfully";
        } else {
            status = HttpStatus.MULTI_STATUS;
            code = 207;
            message = "Some reservations failed to update";
        }

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("success", true);
        responseBody.put("code", code);
        responseBody.put("message", message);
        responseBody.put("data", responseData);

        return new ResponseEntity<>(responseBody, status);
    }


    // 새로 추가된 기능 — QR 코드로 입장 체크인
    @GetMapping("/shows/{showId}/QR")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Object> checkInByQrCode(
            @PathVariable Long showId,
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