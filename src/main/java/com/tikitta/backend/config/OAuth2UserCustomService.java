package com.tikitta.backend.config;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.repository.KakaoOauthRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class OAuth2UserCustomService extends DefaultOAuth2UserService {

    private final KakaoOauthRepository kakaoOauthRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
       //유저 정보를 담은 객체 반환
        OAuth2User user = super.loadUser(userRequest);
        HttpSession session = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        saveOrUpdate(user, session);

        return user;
    }
    //유저가 있으면 정보 업데이트, 없으면 유저 생성
    private KakaoOauth saveOrUpdate(OAuth2User oAuth2User, HttpSession session) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (String) kakaoAccount.get("email");
        String name = (String) profile.get("nickname");

        String selectedRole = (String) session.getAttribute("selectedRole");

        KakaoOauth user = kakaoOauthRepository.findByEmail(email)
                .map(entity -> entity.update(name))
                .orElse(KakaoOauth.builder()
                        .email(email)
                        .name(name)
                        .role(selectedRole != null
                                ? DomainEnums.Role.valueOf(selectedRole)
                                : DomainEnums.Role.USER)
                        .build());

        return kakaoOauthRepository.save(user);
    }
}