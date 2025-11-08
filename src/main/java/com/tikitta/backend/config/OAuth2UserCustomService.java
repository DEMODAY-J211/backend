package com.tikitta.backend.config;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.repository.KakaoOauthRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OAuth2UserCustomService extends DefaultOAuth2UserService {

    private final KakaoOauthRepository kakaoOauthRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        HttpSession session = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest().getSession();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (String) kakaoAccount.get("email");
        String name = (String) profile.get("nickname");

        // --- 👇 [수정된 로직] ---

        // 1. DB 조회를 한 번만 수행합니다.
        Optional<KakaoOauth> userOptional = kakaoOauthRepository.findByEmail(email);
        boolean isSignup;

        KakaoOauth user;
        if (userOptional.isPresent()) {
            // 2-1. 기존 유저인 경우
            user = userOptional.get().update(name);
            isSignup = false;
        } else {
            // 2-2. 신규 유저인 경우
            user = KakaoOauth.builder()
                    .email(email)
                    .name(name)
                    .role(DomainEnums.Role.USER)
                    .visitedPath(DomainEnums.VisitedPath.ETC)
                    .build();
            isSignup = true;
        }

        KakaoOauth savedUser = kakaoOauthRepository.save(user);

        // --- 👆 [수정 완료] ---

        Map<String, Object> customAttributes = new HashMap<>(attributes);
        customAttributes.put("isSignup", isSignup);
        customAttributes.put("userRole", savedUser.getRole().name());

        String roleKey = "ROLE_" + savedUser.getRole().name();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(roleKey)),
                customAttributes,
                userNameAttributeName
        );
    }
}