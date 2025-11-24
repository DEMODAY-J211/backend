package com.tikitta.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CorsConfigurationSource corsConfigurationSource;
    @Value("${frontend-url}")
    private String frontendUrlFromConfig;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        HttpSession session = request.getSession(false);
        String frontendBaseUrl = determineFrontendBaseUrl(request, session);

        String targetUrl;

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        boolean isSignup = (boolean) attributes.getOrDefault("isSignup", false);


            if (isSignup) {
                targetUrl = buildUrl(frontendBaseUrl, "landing?login=success");
            } else {
                Collection<? extends GrantedAuthority> authorities =
                        authentication.getAuthorities();

                boolean isManager = authorities.stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
                boolean isUser = authorities.stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

                if (isManager) {
                    targetUrl = buildUrl(frontendBaseUrl, "homemanager?login=success&role=MANAGER");
                } else if (isUser) {
                    targetUrl = buildUrl(frontendBaseUrl, "homeuser?login=success&role=USER");
                } else {
                    targetUrl = buildUrl(frontendBaseUrl, "landing?login=success&role=NO");
                }
            }


        System.out.println("### 최종 Redirect 될 URL: " + targetUrl + " ###");

        if (session != null) {
            session.removeAttribute(RedirectUriSaveFilter.SAVED_REDIRECT_URI_ATTRIBUTE);
            session.removeAttribute(OriginSaveFilter.SAVED_ORIGIN_ATTRIBUTE); // 사용한 Origin 정보도 세션에서 제거
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
            return redirectUri + "?login=success";
        }
        return null;
    }

    private String buildUrl(String base, String path) {
        if (base == null) {
            base = "";
        }
        // base 뒤에 오는 / 제거
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        if (path == null || path.isEmpty()) {
            return base;
        }

        // path 앞에 / 보장
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return base + path;
    }
    private String trimTrailingSlash(String url) {
        if (url == null) return null;
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String determineFrontendBaseUrl(HttpServletRequest request, HttpSession session) {
        // 1. 세션에 저장된 Origin이 있는지 먼저 확인
        if (session != null) {
            String savedOrigin = (String) session.getAttribute(OriginSaveFilter.SAVED_ORIGIN_ATTRIBUTE);
            if (savedOrigin != null && !savedOrigin.isEmpty()) {
                String trimmed = trimTrailingSlash(savedOrigin);
                System.out.println("### [Origin Check] 세션에 저장된 Origin 사용: " + trimmed + " ###");
                return trimmed;
            }
        }

        if (frontendUrlFromConfig != null && !frontendUrlFromConfig.isEmpty()) {
            String trimmed = trimTrailingSlash(frontendUrlFromConfig);
            System.out.println("### [Origin Check] frontend-url 사용: " + trimmed + " ###");
            return trimmed;
        }

        // 2. 세션에 없다면 (예: 직접 /login/oauth2/code/kakao로 접근), 기존의 폴백 로직 사용
        CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(request);
        List<String> allowedOrigins = corsConfiguration.getAllowedOrigins();
        String defaultOrigin = (allowedOrigins != null && !allowedOrigins.isEmpty()) ? allowedOrigins.get(0) : "";

        System.out.println("### [Origin Check] 세션에 Origin 없음. 기본 URL 사용: " + defaultOrigin + " ###");
        return trimTrailingSlash(defaultOrigin);
    }


}