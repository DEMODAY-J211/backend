package com.tikitta.backend.service;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.KakaoSignupRequest;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final KakaoOauthRepository kakaoOauthRepository;
    private final ManagerRepository managerRepository;
    private final AuthUtil authUtil;
    private final ImageService imageService; // ImageService 주입

    public void signupManager(KakaoSignupRequest request) {
        KakaoOauth kakaoOauth = authUtil.getCurrentUser();
        if (kakaoOauth == null) {
            throw new IllegalStateException("로그인된 유저 정보를 찾을 수 없습니다.");
        }

        Optional<Manager> existingManagerOpt = managerRepository.findByKakaoOauth(kakaoOauth);

        if (existingManagerOpt.isPresent()) {
            // 수정 로직
            Manager manager = existingManagerOpt.get();

            // 이미지 변경 시 기존 이미지 삭제
            if (request.getManagerPicture() != null && !request.getManagerPicture().equals(manager.getPictureUrl())) {
                if (manager.getPictureUrl() != null && !manager.getPictureUrl().isEmpty()) {
                    imageService.delete(manager.getPictureUrl());
                }
                manager.setPictureUrl(request.getManagerPicture());
            }

            manager.setName(request.getManagerName());
            manager.setIntroduction(request.getManagerIntro());
            manager.setDescription(request.getManagerText());
            manager.setUrls(request.getManagerUrl());

            managerRepository.save(manager);

        } else {
            // 생성 로직
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
        
        // 1. DB에서 사용자의 Role을 MANAGER로 설정
        kakaoOauth.setRole(DomainEnums.Role.MANAGER);
        kakaoOauthRepository.save(kakaoOauth);

        // 2. 현재 세션의 권한을 ROLE_MANAGER로 즉시 갱신
        updateUserRoleandSession("MANAGER");
    }

    public void updateUserRoleandSession(String role){
        KakaoOauth currentUser = authUtil.getCurrentUser();

        if(!"USER".equalsIgnoreCase(role)&& !"MANAGER".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        // 1️⃣ DB 반영
        currentUser.setRole(Enum.valueOf(DomainEnums.Role.class, role.toUpperCase()));
        kakaoOauthRepository.save(currentUser);

        // 2️⃣ SecurityContext 즉시 갱신
        var authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + currentUser.getRole().name())
        );

        Map<String, Object> attributes = Map.of(
                "email", currentUser.getEmail(),
                "name", currentUser.getName()
        );

        OAuth2User principal = new DefaultOAuth2User(authorities, attributes, "email");
        Authentication newAuth = new OAuth2AuthenticationToken(principal, authorities, "kakao");
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }
}
