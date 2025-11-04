package com.tikitta.backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * /user/{managerId}/... 경로에 접근할 때 managerId를 세션에 저장하는 필터
 * 로그인 후 해당 매니저의 main 페이지로 리다이렉트하기 위함.
 */
public class ManagerIdSaveFilter implements Filter {

    // "/user/" 뒤의 숫자(managerId)를 추출하는 정규식
    private final Pattern pattern = Pattern.compile("^/user/(\\d+)/.*");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        Matcher matcher = pattern.matcher(uri);

        // 정규식과 일치하는 경우 (예: /user/1/main, /user/12/booking)
        if (matcher.matches()) {
            String managerId = matcher.group(1); // 캡처된 숫자(managerId)
            HttpSession session = httpRequest.getSession(true); // 세션이 없으면 생성
            session.setAttribute("LAST_VISITED_MANAGER_ID", managerId);
        }

        chain.doFilter(request, response);
    }
}