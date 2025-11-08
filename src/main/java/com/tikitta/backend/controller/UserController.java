package com.tikitta.backend.controller;

import com.tikitta.backend.dto.*;
import com.tikitta.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/{managerId}")
public class UserController {

    private final UserService userService;

    @GetMapping("/main")
    public ResponseEntity<ApiResponse<ShowListResponse>> getMainPage(@PathVariable Long managerId,
        Authentication authentication){


        boolean isLoggedIn = authentication != null && authentication.isAuthenticated();

        ShowListResponse data;
        if (isLoggedIn) {
            // 로그인한 사용자 이메일 가져오기
            String userEmail = authentication.getName();
            data = userService.getUserMainPageWithReservationStatus(managerId, userEmail);
        } else {
            data = userService.getUserMainPage(managerId);
        }

        if (data == null || data.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(new ShowListResponse()));
        }

        // 2. ApiResponse 래퍼로 감싸서 반환
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @GetMapping("/detail/{showId}")
    public ResponseEntity<ApiResponse<ShowDetailResponse>> getShowDetail(@PathVariable Long managerId, @PathVariable Long showId){
        ShowDetailResponse data = userService.getShowDetail(showId);
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @GetMapping("/organization")
    public ResponseEntity<ApiResponse<ManagerOrgResponse>> getOrganization(@PathVariable Long managerId){
        ManagerOrgResponse data = userService.getManagerOrg(managerId);
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @GetMapping("/myshow")
    public ResponseEntity<ApiResponse<List<MyReservationItemDto>>> getMyShowReservations(
            @PathVariable Long managerId, // ◀ 쿼리 파라미터
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("로그인된 사용자만 접근할 수 있습니다.");
        }

        List<MyReservationItemDto> data = userService.getMyReservations(managerId, authentication);
        if (data.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(List.of()));
        }
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @GetMapping("/ticket/{reservationId}")
    public ResponseEntity<ApiResponse<MobileTicketResponse>> getMobileTicket(
            @PathVariable Long reservationId,
            Authentication authentication) {

        MobileTicketResponse data = userService.getMobileTicket(reservationId, authentication);

        return ResponseEntity.ok(new ApiResponse<>(data));
    }
}
