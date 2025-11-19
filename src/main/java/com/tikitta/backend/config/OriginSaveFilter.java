package com.tikitta.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OriginSaveFilter extends OncePerRequestFilter {

    public static final String SAVED_ORIGIN_ATTRIBUTE = "SAVED_ORIGIN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // OAuth2 로그인 시작 요청 경로인지 확인
        if (request.getRequestURI().startsWith("/oauth2/authorization/")) {
            String origin = request.getHeader("Origin");
            if (origin != null && !origin.isEmpty()) {
                HttpSession session = request.getSession();
                // 세션에 Origin 헤더 값을 저장
                System.out.println("### [OriginSaveFilter] 로그인 시작. Origin 저장: " + origin + " ###");
                session.setAttribute(SAVED_ORIGIN_ATTRIBUTE, origin);
            }
        }

        filterChain.doFilter(request, response);
    }
}