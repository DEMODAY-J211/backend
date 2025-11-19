package com.tikitta.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CorsConfigurationSource corsConfigurationSource;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String frontendBaseUrl = determineFrontendBaseUrl(request);

        HttpSession session = request.getSession(false);
        String savedRedirectUri = getTargetUrlFromSession(session);
        System.out.println("### [Login Success] 세션에 저장된 Redirect URI: " + savedRedirectUri + " ###");

        String targetUrl;

        if (savedRedirectUri != null) {
            if (savedRedirectUri.startsWith("/")) {
                targetUrl = frontendBaseUrl + savedRedirectUri;
            } else {
                targetUrl = savedRedirectUri;
            }
        } else {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            boolean isSignup = (boolean) attributes.getOrDefault("isSignup", false);
            String role = (String) attributes.getOrDefault("userRole", "USER");

            if (isSignup) {
                targetUrl = frontendBaseUrl + "/landing";
            } else {
                if ("MANAGER".equalsIgnoreCase(role)) {
                    targetUrl = frontendBaseUrl + "/homemanager" + "?login=success";
                } else {
                    targetUrl = frontendBaseUrl + "/homeuser" + "?login=success";
                }
            }
        }

        System.out.println("### 최종 Redirect 될 URL: " + targetUrl + " ###");

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
            return redirectUri + "?login=success";
        }
        return null;
    }

    private String determineFrontendBaseUrl(HttpServletRequest request) {
        CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(request);
        List<String> allowedOrigins = corsConfiguration.getAllowedOrigins();

        String origin = request.getHeader("Origin");

        if (origin != null && allowedOrigins != null && allowedOrigins.contains(origin)) {
            System.out.println("### [Origin Check] 요청 Origin: " + origin + " -> 허용됨 ###");
            return origin;
        }

        // 허용된 Origin이 아니거나 Origin 헤더가 없는 경우, 목록의 첫 번째 값을 기본값으로 사용
        String defaultOrigin = (allowedOrigins != null && !allowedOrigins.isEmpty()) ? allowedOrigins.get(0) : "/";
        System.out.println("### [Origin Check] 요청 Origin: " + (origin != null ? origin : "null") + " -> 허용되지 않음. 기본 URL 사용: " + defaultOrigin + " ###");
        return defaultOrigin;
    }
}