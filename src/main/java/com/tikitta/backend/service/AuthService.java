package com.tikitta.backend.service;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.KakaoSignupRequest;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final KakaoOauthRepository kakaoOauthRepository;
    private final ManagerRepository managerRepository;

    public void signupManager(KakaoSignupRequest request) {
        KakaoOauth kakaoOauth = kakaoOauthRepository.findById(request.getKakaoOauthId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Oauth ID입니다."));

        Manager manager = Manager.builder()
                .kakaoOauth(kakaoOauth)
                .name(request.getManagerName())
                .pictureUrl(request.getManagerPicture())
                .introduction(request.getManagerIntro())
                .description(request.getManagerText())
                .urls(request.getManagerUrl())
                .build();

        managerRepository.save(manager);

    }
}
