package com.tikitta.backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 사용자가 로그인하지 않은 상태에서 /user/main/{managerId} 등
 * 특정 페이지에 접근하면, 원래 URL을 세션에 저장해두었다가
 * 로그인 성공 후 다시 그 페이지로 복귀시키는 필터.
 */
@Component
public class SaveRequestUrlFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession();

        // 현재 로그인 상태 확인. 밑에 주석처리된거 실제 서비스때 풀기
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       boolean isAuthenticated = (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal()));


        // 로그인 안 된 상태에서 /user/main 으로 접근한 경우 URL 저장
        String uri = httpRequest.getRequestURI();
        if (!isAuthenticated && uri.startsWith("/user/")) {
            session.setAttribute("prevUrl", uri);
        }

        chain.doFilter(request, response);
    }
}