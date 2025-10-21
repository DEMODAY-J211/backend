package com.tikitta.backend.controller;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.ApiResponse;
import com.tikitta.backend.dto.MyShowListResponseDto;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.service.ShowService; // ShowService import 추가
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // PreAuthorize import 추가
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
    private final ShowService showService; // ShowService 주입

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

    @GetMapping("/shows/list")
    @PreAuthorize("hasRole('MANAGER')") // MANAGER 역할만 접근 가능
    public ResponseEntity<ApiResponse<MyShowListResponseDto>> getMyShows() {
        MyShowListResponseDto myShowList = showService.getMyShows();
        return ResponseEntity.ok(new ApiResponse<>(myShowList));
    }
}
