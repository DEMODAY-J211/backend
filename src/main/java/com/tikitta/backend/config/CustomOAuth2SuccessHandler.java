package com.tikitta.backend.config;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.repository.KakaoOauthRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final KakaoOauthRepository kakaoOauthRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // 🔹 카카오 로그인 성공 시 사용자 정보 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");

        // 🔹 DB에서 사용자 조회
        KakaoOauth user = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        // 🔹 세션에서 값 가져오기
        HttpSession session = request.getSession(false);
        String selectedRole = null;
        Boolean isSignup = false;

        if (session != null) {
            selectedRole = (String) session.getAttribute("selectedRole"); // "USER" or "MANAGER"
            isSignup = (Boolean) session.getAttribute("isSignup");        // true or false
        }

        // 🔹 최종 리다이렉트 경로
        String redirectUrl = "/auth/kakao/signup"; // 기본값
        String prevUrl = (session != null) ? (String) session.getAttribute("prevUrl") : null;

        // ✅ [회원가입 플로우]
        if (Boolean.TRUE.equals(isSignup)) {
            if ("MANAGER".equalsIgnoreCase(selectedRole)) {
                redirectUrl = "/auth/kakao/manager";
            } else if ("USER".equalsIgnoreCase(selectedRole)) {
                redirectUrl = (prevUrl != null) ? prevUrl : "/user/main";
                if(prevUrl != null) {
                    session.removeAttribute("prevUrl");
            }
        }

        // ✅ [로그인 플로우]
        else {
            if (prevUrl != null) {
                redirectUrl = prevUrl;
                session.removeAttribute("prevUrl");
            } else if (user.getRole() != null) {
                switch (user.getRole()) {
                    case MANAGER -> redirectUrl = "/manager/main";
                    case USER -> redirectUrl = "/user/main";
                }
            }
        }

        // 🔹 세션 정리 (선택)
        if (session != null) {
            session.removeAttribute("selectedRole");
            session.removeAttribute("isSignup");
        }

        // 🔹 리다이렉트 실행
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
}

