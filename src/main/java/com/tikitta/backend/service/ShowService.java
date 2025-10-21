package com.tikitta.backend.service;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.domain.Shows;
import com.tikitta.backend.dto.MyShowItemDto;
import com.tikitta.backend.dto.MyShowListResponseDto;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.repository.ShowsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowService {

    private final ManagerRepository managerRepository;
    private final KakaoOauthRepository kakaoOauthRepository;
    private final ShowsRepository showsRepository;

    public MyShowListResponseDto getMyShows() {
        // 1. 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof OAuth2User)) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
        }
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oauth2User.getAttributes().get("email");

        // 2. 이메일을 통해 Manager 엔티티 조회
        KakaoOauth kakaoOauth = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다."));
        Manager manager = managerRepository.findByKakaoOauth(kakaoOauth)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        // 3. 매니저의 모든 공연 목록 조회
        List<Shows> allShows = showsRepository.findByManager(manager); // findByManager로 수정

        // 4. DRAFT 상태의 공연 존재 여부 확인
        boolean hasDraft = allShows.stream()
                .anyMatch(show -> show.getStatus() == DomainEnums.ShowStatus.DRAFT);

        // 5. PUBLISHED 상태의 공연만 필터링하여 DTO로 변환
        List<MyShowItemDto> publishedShows = allShows.stream()
                .filter(show -> show.getStatus() == DomainEnums.ShowStatus.PUBLISHED)
                .map(MyShowItemDto::fromEntity)
                .collect(Collectors.toList());

        // 6. 최종 응답 DTO 생성 및 반환
        return new MyShowListResponseDto(hasDraft, publishedShows);
    }
}
