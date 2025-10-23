package com.tikitta.backend.service;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.CustomerListResponseDto;
import com.tikitta.backend.dto.MyShowItemDto;
import com.tikitta.backend.dto.MyShowListResponseDto;
import com.tikitta.backend.dto.ReservationDetailDto;
import com.tikitta.backend.dto.ShowTimeInfo;
import com.tikitta.backend.repository.KakaoOauthRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.repository.ReservationRepository;
import com.tikitta.backend.repository.ShowsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowService {

    private final ManagerRepository managerRepository;
    private final KakaoOauthRepository kakaoOauthRepository;
    private final ShowsRepository showsRepository;
    private final ReservationRepository reservationRepository; // Repository 추가

    public MyShowListResponseDto getMyShows() {
        // ... (기존 getMyShows 메소드)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof OAuth2User)) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
        }
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oauth2User.getAttributes().get("email");

        KakaoOauth kakaoOauth = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다."));
        Manager manager = managerRepository.findByKakaoOauth(kakaoOauth)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        List<Shows> allShows = showsRepository.findByManager(manager);

        boolean hasDraft = allShows.stream()
                .anyMatch(show -> show.getStatus() == DomainEnums.ShowStatus.DRAFT);

        List<MyShowItemDto> publishedShows = allShows.stream()
                .filter(show -> show.getStatus() == DomainEnums.ShowStatus.PUBLISHED)
                .map(MyShowItemDto::fromEntity)
                .collect(Collectors.toList());

        return new MyShowListResponseDto(hasDraft, publishedShows);
    }

    // 새로 추가된 메소드
    public CustomerListResponseDto getReservationList(Long showId, Long showtimeId) {
        // 1. 매니저 인증 및 공연 소유권 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof OAuth2User)) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
        }
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oauth2User.getAttributes().get("email");

        KakaoOauth kakaoOauth = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다."));
        Manager manager = managerRepository.findByKakaoOauth(kakaoOauth)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 ID입니다: " + showId));

        if (!show.getManager().getId().equals(manager.getId())) {
            throw new AccessDeniedException("해당 공연에 대한 접근 권한이 없습니다.");
        }

        // 2. 회차 목록 조회 및 정렬
        List<ShowTime> allShowTimes = show.getShowTimes().stream()
                .sorted(Comparator.comparing(ShowTime::getStartAt))
                .collect(Collectors.toList());

        if (allShowTimes.isEmpty()) {
            throw new IllegalArgumentException("해당 공연에 등록된 회차가 없습니다.");
        }

        // 3. 조회할 회차 결정 (파라미터 또는 기본값)
        ShowTime selectedShowTime;
        if (showtimeId != null) {
            selectedShowTime = allShowTimes.stream()
                    .filter(st -> st.getId().equals(showtimeId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차 ID입니다: " + showtimeId));
        } else {
            selectedShowTime = allShowTimes.get(0); // 쿼리 파라미터 없으면 가장 빠른 회차를 기본값으로
        }

        // 4. 예매 목록 조회
        List<Reservation> reservations = reservationRepository.findByShowTimeWithDetailsOrderByCreatedAtDesc(selectedShowTime);

        // 5. DTO로 변환
        List<ShowTimeInfo> showTimeInfoList = allShowTimes.stream()
                .map(ShowTimeInfo::fromEntity)
                .collect(Collectors.toList());

        List<ReservationDetailDto> reservationDetailDtoList = reservations.stream()
                .map(ReservationDetailDto::fromEntity)
                .collect(Collectors.toList());

        // 6. 최종 응답 DTO 생성 및 반환
        return new CustomerListResponseDto(
                showTimeInfoList,
                selectedShowTime.getStartAt(),
                selectedShowTime.getId(),
                reservationDetailDtoList
        );
    }
}
