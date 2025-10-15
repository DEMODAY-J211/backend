package com.tikitta.backend.controller;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.KakaoSignupRequest;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/kakao")
public class AuthController {

    private final KakaoOauthRepository kakaoOauthRepository;
    private final ManagerRepository managerRepository;
    private final AuthService authService;

    @PostMapping("/select-role")
    public ResponseEntity<String> selectRole(
            @RequestParam String role,
            HttpSession session) {

        // 선택한 role만 저장 (회원가입 여부는 없어도 됨)
        session.setAttribute("selectedRole", role.toUpperCase());
        return ResponseEntity.ok("Role saved to session");
    }

    @PostMapping("/manager")
    public ResponseEntity<String> signupManager(@RequestBody KakaoSignupRequest kakaoSignupRequest) {
        authService.signupManager(kakaoSignupRequest);
        return ResponseEntity.ok("관리자 회원가입 완료");
    }

}
