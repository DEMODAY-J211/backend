package com.tikitta.backend.controller;

import com.tikitta.backend.dto.*;
import com.tikitta.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/{managerId}")
public class UserController {

    private final UserService userService;

    @GetMapping("/main")
    public ResponseEntity<ApiResponse<ShowListResponse>> getMainPage(@PathVariable Long managerId){

        // 1. Service를 호출하여 DTO 데이터 받기
        ShowListResponse data = userService.getUserMainPage(managerId);

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

}
