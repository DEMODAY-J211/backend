package com.tikitta.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class RedirectUriSaveFilter extends OncePerRequestFilter {

    public static final String REDIRECT_URI_PARAM = "redirect_uri";
    public static final String SAVED_REDIRECT_URI_ATTRIBUTE = "USER_REDIRECT_URI";

    @Value("${frontend-url}")
    private String authorizedRedirectBaseUrl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // OAuth2 로그인 요청일 때만 동작
        if (request.getRequestURI().startsWith("/oauth2/authorization/")) {
            System.out.println("\n--- RedirectUriSaveFilter 동작 ---");
            System.out.println("요청 URI: " + request.getRequestURI());

            String redirectUri = request.getParameter(REDIRECT_URI_PARAM);
            System.out.println("redirect_uri 파라미터 값: " + redirectUri);
            System.out.println("허용된 Base URL: " + authorizedRedirectBaseUrl);

            // redirect_uri 파라미터가 있고, 허용된 URL 형식인지 확인
            if (redirectUri != null && !redirectUri.isEmpty()) {
                if (isAuthorizedRedirectUri(redirectUri)) {
                    request.getSession().setAttribute(SAVED_REDIRECT_URI_ATTRIBUTE, redirectUri);
                    System.out.println("세션에 Redirect URI 저장 성공: " + redirectUri);
                } else {
                    System.out.println("저장 실패: 허용되지 않은 Redirect URI 입니다.");
                }
            } else {
                System.out.println("저장 안함: redirect_uri 파라미터가 없거나 비어있습니다.");
            }
            System.out.println("---------------------------------\n");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Open Redirect 취약점을 방지하기 위해, 리다이렉트 될 URL이 허가된 URL인지 검증합니다.
     */
    private boolean isAuthorizedRedirectUri(String uri) {
        return uri.startsWith(authorizedRedirectBaseUrl);
    }
}