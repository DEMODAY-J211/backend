package com.tikitta.backend.controller;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final KakaoOauthRepository kakaoOauthRepository;
    private final ManagerRepository managerRepository;

    @GetMapping("/link")
    public ResponseEntity<String> getManagerLink(Authentication authentication) {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = (String) oAuth2User.getAttributes().get("email");

        KakaoOauth kakaoOauth = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 Oauth 정보를 찾을 수 없습니다."));

        Manager manager = managerRepository.findByKakaoOauth(kakaoOauth)
                .orElseThrow(() -> new RuntimeException("매니저 정보를 찾을 수 없습니다. (매니저 회원가입이 완료되지 않았을 수 있습니다)"));

        String managerLink = "user/" + manager.getId() + "/main";
        return ResponseEntity.ok(managerLink);
    }
}