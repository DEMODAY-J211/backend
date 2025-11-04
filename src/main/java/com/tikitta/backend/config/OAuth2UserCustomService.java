package com.tikitta.backend.config;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.repository.KakaoOauthRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // ◀ Import
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User; // ◀ Import
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections; // ◀ Import
import java.util.HashMap; // ◀ Import
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OAuth2UserCustomService extends DefaultOAuth2UserService {

    private final KakaoOauthRepository kakaoOauthRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. 카카오로부터 유저 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);
        HttpSession session = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest().getSession();

        // 2. 카카오 정보 파싱
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (String) kakaoAccount.get("email");
        String name = (String) profile.get("nickname");
        String selectedRole = (String) session.getAttribute("selectedRole");
        // TODO: 프론트엔드에서 visitedPath를 세션에 저장해주는 로직 추가 필요
        String visitedPathStr = (String) session.getAttribute("visitedPath");

        // 3. DB에서 유저 조회 또는 신규 생성
        boolean isSignup = !kakaoOauthRepository.findByEmail(email).isPresent(); // ◀ 신규 유저인지 확인

        KakaoOauth user = kakaoOauthRepository.findByEmail(email)
                .map(entity -> entity.update(name)) // 기존 회원이면 이름 업데이트
                .orElseGet(() -> KakaoOauth.builder() // 신규 회원이면
                        .email(email)
                        .name(name)
                        .role(DomainEnums.Role.USER)
                        .visitedPath(DomainEnums.VisitedPath.ETC) // ◀ 기본값 (null 방지)
                        .build());

        KakaoOauth savedUser = kakaoOauthRepository.save(user);

        // 4. Spring Security가 사용할 OAuth2User 객체 재구성

        // 4-1. 기존 attributes 복사 (kakaoId 등 중요 정보 포함)
        Map<String, Object> customAttributes = new HashMap<>(attributes);

        // 4-2. 'isSignup'과 'role' 정보를 추가
        customAttributes.put("isSignup", isSignup);
        customAttributes.put("userRole", savedUser.getRole().name()); // "USER" 또는 "MANAGER"

        // 4-3. 권한 설정
        String roleKey = "ROLE_" + savedUser.getRole().name();

        // 4-4. user-name-attribute (고유 식별자)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 5. 'isSignup' 정보가 담긴 새로운 DefaultOAuth2User 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(roleKey)),
                customAttributes, // ◀ 'isSignup'이 포함된 customAttributes
                userNameAttributeName
        );
    }
}