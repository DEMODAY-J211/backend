package com.tikitta.backend.controller;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.ApiResponse;
import com.tikitta.backend.dto.CustomerListResponseDto;
import com.tikitta.backend.dto.MyShowListResponseDto;
import com.tikitta.backend.dto.ReservationSeatListResponse;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final KakaoOauthRepository kakaoOauthRepository;
    private final ManagerRepository managerRepository;
    private final ShowService showService;

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
        // keyword가 없는 검색이므로, searchReservationList에 null을 전달하여 호출
        CustomerListResponseDto reservationList = showService.searchReservationList(showId, showtimeId, null);
        return ResponseEntity.ok(new ApiResponse<>(reservationList));
    }

    // ▼▼▼ 새로 추가된 검색 엔드포인트 ▼▼▼
    @GetMapping("/shows/{showId}/customer/search")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<CustomerListResponseDto>> searchShowCustomers(
            @PathVariable Long showId,
            @RequestParam(required = false) Long showtimeId,
            @RequestParam(required = false) String keyword) {
        CustomerListResponseDto reservationList = showService.searchReservationList(showId, showtimeId, keyword);
        return ResponseEntity.ok(new ApiResponse<>(reservationList));
    }
    // ▲▲▲ 새로 추가된 검색 엔드포인트 ▲▲▲

    //좌석별 조회
    @GetMapping("/{showId}/seats")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<ReservationSeatListResponse>>> getShowSeats(
            @PathVariable Long showId,
            @RequestParam Long showtimeId
    ){
        List<ReservationSeatListResponse> seatList = showService.getReservationSeatList(showtimeId);
        return ResponseEntity.ok(new ApiResponse<>(seatList));
    }
}
