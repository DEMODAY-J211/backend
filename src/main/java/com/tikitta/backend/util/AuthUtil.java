package com.tikitta.backend.util;


import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.repository.KakaoOauthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthUtil {

    private final KakaoOauthRepository kakaoOauthRepository;

    // AuthUtil.java
    public KakaoOauth getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof OAuth2User)) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // --- 👇 [수정된 부분] ---
        // 1. 전체 속성 맵 가져오기
        Map<String, Object> attributes = oauth2User.getAttributes();
        // 2. 'kakao_account' 맵 가져오기
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        // 3. 'kakao_account'에서 실제 이메일 가져오기
        String email = (String) kakaoAccount.get("email");
        // --- 👆 [수정 완료] ---

        if (email == null) {
            throw new IllegalArgumentException("카카오 계정에서 이메일 정보를 찾을 수 없습니다.");
        }

        return kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다: " + email));
    }


}