package com.tikitta.backend.controller;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.dto.KakaoSignupRequest;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/kakao")
public class AuthController {

    private final AuthService authService;
    private final KakaoOauthRepository kakaoOauthRepository;

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

    // 개발용 임시 로그인 API
    @PostMapping("/dev-login")
    public ResponseEntity<String> devLogin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        KakaoOauth user = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        Map<String, Object> attributes = Map.of(
                "email", user.getEmail(),
                "name", user.getName()
        );
        String nameAttributeKey = "email";

        OAuth2User oAuth2UserPrincipal = new DefaultOAuth2User(authorities, attributes, nameAttributeKey);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                oAuth2UserPrincipal,
                null,
                authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return ResponseEntity.ok("Successfully logged in as " + email);
    }
}
