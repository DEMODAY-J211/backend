package com.tikitta.backend.controller;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.dto.KakaoSignupRequest;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.service.AuthService;
import com.tikitta.backend.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
    private final AuthUtil authUtil; // 3. AuthUtil 주입

    @PostMapping("/select-role")
    @Transactional // 4. DB 변경이 있으므로 Transactional 추가
    public ResponseEntity<String> selectRole(
            @RequestParam String role,
            HttpSession session) { // (HttpSession은 이제 사용하지 않지만 호환성을 위해 둠)

        // 5. 현재 로그인한 사용자 정보를 가져옵니다.
        KakaoOauth currentUser = authUtil.getCurrentUser();

        // 6. 요청된 역할(role)에 따라 DB 업데이트
        if ("MANAGER".equalsIgnoreCase(role)) {
            currentUser.setRole(DomainEnums.Role.MANAGER); // (KakaoOauth 엔티티에 @Setter 추가 필요)
            kakaoOauthRepository.save(currentUser);
            return ResponseEntity.ok("MANAGER role updated.");

        } else if ("USER".equalsIgnoreCase(role)) {
            // (USER는 기본값이므로 사실상 필요 없지만 로직상 추가)
            currentUser.setRole(DomainEnums.Role.USER);
            kakaoOauthRepository.save(currentUser);
            return ResponseEntity.ok("USER role selected.");

        } else {
            return ResponseEntity.badRequest().body("Invalid role.");
        }
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

        // OAuth2AuthenticationToken 생성
        Authentication authentication = new OAuth2AuthenticationToken(
                oAuth2UserPrincipal,
                authorities,
                "kakao" // authorizedClientRegistrationId
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return ResponseEntity.ok("Successfully logged in as " + email);
    }

    @PatchMapping("/update-role")
    public ResponseEntity<String> updateRole(@RequestBody Map<String, String> request){
        String role= request.get("role");
        authService.updateUserRoleandSession(role);
        return ResponseEntity.ok("Role updated to " + role.toUpperCase() );
    }

}