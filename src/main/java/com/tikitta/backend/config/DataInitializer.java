package com.tikitta.backend.config;

import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final ManagerRepository managerRepository;
    private final KakaoOauthRepository kakaoOauthRepository;
}
