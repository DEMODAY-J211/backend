package com.tikitta.backend.controller;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.*;
import com.tikitta.backend.dto.ApiResponse;
import com.tikitta.backend.dto.CustomerListResponseDto;
import com.tikitta.backend.dto.MyShowListResponseDto;
import com.tikitta.backend.dto.QrReadResponseDto;
import com.tikitta.backend.dto.ReservationStatusUpdateRequest;
import com.tikitta.backend.dto.ReservationStatusUpdateResponse;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.service.CheckInService;
import com.tikitta.backend.service.ShowDraftService;
import com.tikitta.backend.service.LocationService;
import com.tikitta.backend.service.ManagerService;
import com.tikitta.backend.service.ShowService;
import java.util.LinkedHashMap;

import com.tikitta.backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final KakaoOauthRepository kakaoOauthRepository;
    private final ManagerRepository managerRepository;
    private final ShowService showService;
    private final CheckInService checkInService;
    private final AuthUtil authUtil; // 2. AuthUtil 주입
    private final ManagerService managerService;
    private final LocationService locationService;
    private final ShowDraftService showDraftService;

    @GetMapping("/main")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> getManagerMain(Authentication authentication) {
        // 3. AuthUtil을 사용해 현재 유저(KakaoOauth)를 가져옵니다.
        KakaoOauth user = authUtil.getCurrentUser();

        // 4. KakaoOauth 정보로 Manager 정보를 조회합니다.
        Manager manager = managerRepository.findByKakaoOauth(user)
                .orElseThrow(() -> new RuntimeException("매니저 정보를 찾을 수 없습니다."));

        // 5. 매니저 이름을 반환합니다.
        return ResponseEntity.ok(new ApiResponse<>(manager.getName()));
    }

    // ManagerController.java
    @GetMapping("/link")
    public ResponseEntity<String> getManagerLink(Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // --- 👇 [수정된 부분] ---
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        // --- 👆 [수정 완료] ---

        if (email == null) {
            throw new RuntimeException("로그인된 Oauth 정보를 찾을 수 없습니다. (이메일 없음)");
        }

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

    //좌석별 조회
    @GetMapping("/shows/{showId}/checkin")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<ReservationSeatListResponse>>> getShowSeats(
            @PathVariable Long showId,
            @RequestParam Long showtimeId){
        List<ReservationSeatListResponse> seatList = showService.getReservationSeatList(showtimeId);
        return ResponseEntity.ok(new ApiResponse<>(seatList));
    }

    //좌석별 상태 수정
    @PatchMapping("/shows/{showId}/checkin")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<CheckinStatusUpdateResponse>> updateCheckinStatus(
            @PathVariable Long showId,
            @RequestParam Long showtimeId,
            @RequestBody CheckinStatusUpdateRequest request
    ){
        CheckinStatusUpdateResponse response=showService.updateCheckinStatus(showId,showtimeId,request);

        if (response.getFailedIds().isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(200, "실패없이 모두 업데이트되었습니다.", response));
        } else {
            return ResponseEntity.status(207)
                    .body(new ApiResponse<>(207, "업데이트에 실패한 예약건이 존재합니다.", response));
        }
    }

    @GetMapping("/shows/{showId}/checkin/search")
    public ResponseEntity<List<ReservationSeatListResponse>> getReservationSeatList(
            @PathVariable("showId") Long showId,
            @RequestParam(value = "showtimeId", required = false) Long showtimeId,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        List<ReservationSeatListResponse> response = showService.getReservationSeatList(showId, showtimeId, keyword);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shows/venues")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<LocationLikeResponse>>> getLikedLocations() {
        List<LocationLikeResponse> likedLocations = managerService.getLikedLocations();
        return ResponseEntity.ok(new ApiResponse<>(likedLocations));
    }

    @GetMapping("/venue/view")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<LocationViewResponse>>> getAllLocations() {
        List<LocationViewResponse> locations = locationService.getAllLocations();
        return ResponseEntity.ok(new ApiResponse<>(locations));
    }

//------등록된 공연 수정------//

    @GetMapping("shows/{showId}/edit")
    public ResponseEntity<ApiResponse<ShowDraftResponse>> getShow(
            @PathVariable Long showId
    ){
        ShowDraftResponse response =showService.getPublishShow(showId);
        return ResponseEntity.ok(new ApiResponse<>(200,"등록된 공연 조회 성공",response));
    }


    @PatchMapping("/shows/{showId}/edit")
    public ResponseEntity<ApiResponse<ShowUpdateResponse>> updatePublishedShow(
            @PathVariable Long showId,
            @RequestBody ShowPublishUpdateRequest request
    ) {
        ShowUpdateResponse response = showService.updatePublishedShow(showId, request);
        return ResponseEntity.ok(new ApiResponse<>(200,"예매중인 공연 수정 완료",response));
    }

    //---------임시저장-------------//
    @PostMapping("/shows")
    public ResponseEntity<ApiResponse<ShowUpdateResponse>> createShow(){
        ShowUpdateResponse response= showDraftService.CreateShow();
        return ResponseEntity.ok(new ApiResponse<>(200,"초안 작성 성공",response));
    }

    @PatchMapping("/shows/{showId}/draft")
    public ResponseEntity<ApiResponse<ShowUpdateResponse>> updateDraft(
        @PathVariable("showId") Long showId,
        @RequestBody ShowUpdateRequest request){

        ShowUpdateResponse response =showDraftService.updateShow(showId, request);

        return ResponseEntity.ok(new ApiResponse<>(200,"임시저장 되었습니다.",response));

    }

    @PostMapping("/shows/{showId}/publish")
    public ResponseEntity<ApiResponse<ShowUpdateResponse>> updatePublish(
            @PathVariable("showId") Long showId
    ){
        ShowUpdateResponse response = showDraftService.publishShow(showId);

        return ResponseEntity.ok(new ApiResponse<>(200,"최종 등록 완료되었습니다.",response));
    }

    @GetMapping("/shows/{showId}/draft")
    public ResponseEntity<ApiResponse<ShowDraftResponse>> getDraft(
            @PathVariable Long showId
    ){
        ShowDraftResponse response =showDraftService.getShowDraft(showId);
        return ResponseEntity.ok(new ApiResponse<>(200,"임시저장 미리보기",response));
    }

    @DeleteMapping("/shows/draft")
    public ResponseEntity<ApiResponse<ShowDraftDeleteResponse>> deleteShowDraft() {

        ShowDraftDeleteResponse response = showDraftService.deleteShowDraft();

        return ResponseEntity.ok(new ApiResponse(200,"임시저장 공연이 삭제되었습니다.", response)
        );
    }


}