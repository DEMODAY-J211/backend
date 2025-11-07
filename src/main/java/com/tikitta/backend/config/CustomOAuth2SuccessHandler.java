package com.tikitta.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String frontendBaseUrl = "http://localhost:5173";

        // 1. Service가 반환한 '커스텀' OAuth2User 객체 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 2. Service가 넣어준 속성 꺼내기 (안전하게)
        Object isSignupObj = attributes.get("isSignup");
        boolean isSignup = (isSignupObj instanceof Boolean) ? (Boolean) isSignupObj : false;

        Object roleObj = attributes.get("userRole");
        String role = (roleObj instanceof String) ? (String) roleObj : "USER"; // 기본값 USER

        String redirectUrl;

        // 1. [회원가입 플로우]
        if (isSignup) {
            redirectUrl = frontendBaseUrl + "/landing";
        } else {
            if ("MANAGER".equalsIgnoreCase(role)) {
                redirectUrl = frontendBaseUrl + "/manageshow";
            } else {
                String relativePath = getDefaultUserRedirectUrl(request.getSession(false));
                redirectUrl = frontendBaseUrl + "/myticketlist";
            }
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("selectedRole");
            session.removeAttribute("LAST_VISITED_MANAGER_ID");
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }


    /**
     * USER의 리다이렉트 URL을 결정하는 헬퍼 메소드
     */
    private String getDefaultUserRedirectUrl(HttpSession session) {
        String managerId = null;
        if (session != null) {
            // ManagerIdSaveFilter가 저장한 세션 값 확인
            Object managerIdObj = session.getAttribute("LAST_VISITED_MANAGER_ID");
            if (managerIdObj instanceof String) {
                managerId = (String) managerIdObj;
            }
        }

        if (managerId != null) {
            // 저장된 managerId가 있으면 해당 main으로
            return "/user/" + managerId + "/main";
        } else {
            // (예외 상황) 저장된 managerId가 없으면 테스트용 1번으로
            return "/myticketlist"; // (또는 "/" 루트 페이지)
        }
    }
}