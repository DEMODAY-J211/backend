package com.tikitta.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${frontend-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        HttpSession session = request.getSession(false);

        // 세션에서 redirect_uri 값을 가져와 콘솔에 출력 (디버깅용)
        String sessionRedirectUri = getTargetUrlFromSession(session);
        System.out.println("### [Login Success] 세션에 저장된 Redirect URI: " + sessionRedirectUri + " ###");

        String targetUrl = sessionRedirectUri;

        if (targetUrl == null) {
            // 세션에 저장된 리다이렉트 URI가 없으면 기존 로직 수행
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            boolean isSignup = (boolean) attributes.getOrDefault("isSignup", false);
            String role = (String) attributes.getOrDefault("userRole", "USER");

            if (isSignup) {
                targetUrl = frontendBaseUrl + "/landing";
            } else {
                if ("MANAGER".equalsIgnoreCase(role)) {
                    targetUrl = frontendBaseUrl + "/homemanager";
                } else {
                    targetUrl = frontendBaseUrl + "/homeuser";
                }
            }
        }

        // 사용한 세션 속성 정리
        if (session != null) {
            session.removeAttribute(RedirectUriSaveFilter.SAVED_REDIRECT_URI_ATTRIBUTE);
            session.removeAttribute("selectedRole");
            session.removeAttribute("LAST_VISITED_MANAGER_ID");
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getTargetUrlFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        String redirectUri = (String) session.getAttribute(RedirectUriSaveFilter.SAVED_REDIRECT_URI_ATTRIBUTE);
        if (redirectUri != null && !redirectUri.isEmpty()) {
            return redirectUri;
        }
        return null;
    }
}