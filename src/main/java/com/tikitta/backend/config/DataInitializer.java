package com.tikitta.backend.config;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner; // ◀ import 추가
import org.springframework.stereotype.Component;
import java.time.LocalDateTime; // ◀ import 추가

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner { // ◀ CommandLineRunner 구현

    private final ManagerRepository managerRepository;
    private final KakaoOauthRepository kakaoOauthRepository;

    @Override
    public void run(String... args) throws Exception {
        String testEmail = "test_manager@kakao.com";

        // 1. 중복 생성을 막기 위해 이메일로 먼저 확인
        if (kakaoOauthRepository.findByEmail(testEmail).isEmpty()) {

            // 2. 테스트용 KakaoOauth 계정 생성 (MANAGER 역할)
            KakaoOauth testOauth = KakaoOauth.builder()
                    .email(testEmail)
                    .name("테스트매니저")
                    .role(DomainEnums.Role.MANAGER)
                    .createdAt(LocalDateTime.now()) // ◀ KakaoOauth의 필수 필드 채우기
                    .visitedPath(DomainEnums.VisitedPath.ETC) // ◀ KakaoOauth의 필수 필드 채우기
                    .build();
            kakaoOauthRepository.save(testOauth);

            // 3. 위 Oauth 계정과 연결된 Manager 계정 생성
            Manager testManager = Manager.builder()
                    .kakaoOauth(testOauth)
                    .name("테스트공연기획사")
                    .introduction("테스트용 매니저 소개입니다.")
                    .build();
            managerRepository.save(testManager);

            System.out.println("=========================================");
            System.out.println("테스트 매니저 데이터 생성 완료: " + testEmail);
            System.out.println("=========================================");
        }
    }
}