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

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Object isSignupObj = attributes.get("isSignup");
        boolean isSignup = (isSignupObj instanceof Boolean) ? (Boolean) isSignupObj : false;

        Object roleObj = attributes.get("userRole");
        String role = (roleObj instanceof String) ? (String) roleObj : "USER";

        String redirectUrl;

        if (isSignup) {
            redirectUrl = frontendBaseUrl + "/select-role";
        } else {
            if ("MANAGER".equalsIgnoreCase(role)) {
                redirectUrl = frontendBaseUrl + "/manager/main";
            } else {
                String relativePath = getDefaultUserRedirectUrl(request.getSession(false));
                redirectUrl = frontendBaseUrl + relativePath;
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

    private String getDefaultUserRedirectUrl(HttpSession session) {
        String managerId = null;
        if (session != null) {
            Object managerIdObj = session.getAttribute("LAST_VISITED_MANAGER_ID");
            if (managerIdObj instanceof String) {
                managerId = (String) managerIdObj;
            }
        }

        if (managerId != null) {
            return "/user/" + managerId + "/main";
        } else {
            return "/user/1/main";
        }
    }
}