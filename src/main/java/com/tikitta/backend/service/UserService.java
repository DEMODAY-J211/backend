package com.tikitta.backend.service;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.*;
import com.tikitta.backend.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final ManagerRepository managerRepository;
    private final ShowsRepository showsRepository;
    private final ReservationRepository reservationRepository;
    private final KakaoOauthRepository kakaoOauthRepository;

    public ShowListResponse getUserMainPage(Long managerId){
// 1. managerId로 Manager 엔티티 조회
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매니저입니다. ID: " + managerId));

        // 2. Manager 엔티티로 연관된 Shows 리스트 조회 (1단계에서 추가한 메소드 사용)
        List<Shows> showsList = showsRepository.findByManager(manager);

        // 3. List<Shows>를 List<ShowItemResponse>로 변환
        List<ShowItemDto> showItemList = showsList.stream()
                .map(ShowItemDto::new) // ◀ DTO 생성자 호출
                .collect(Collectors.toList());

        // 4. ShowListResponse DTO로 조립하여 반환
        return new ShowListResponse(manager, showItemList);
    }

    public ShowDetailResponse getShowDetail(Long showId){
        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다. ID: " + showId));
        return new ShowDetailResponse(show);
    }

    public ManagerOrgResponse getManagerOrg(Long managerId){
        // 1. managerId로 Manager 엔티티 조회
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매니저입니다. ID: " + managerId));

        // 2. DTO로 변환하여 반환
        return new ManagerOrgResponse(manager);
    }

    public List<MyReservationItemDto> getMyReservations(Long managerId, String status, Authentication authentication) {

        // 1. 로그인 사용자 정보 확인
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");
        KakaoOauth user = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 사용자 정보를 찾을 수 없습니다."));

        // 2. 필터링할 매니저 정보 확인
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매니저 ID입니다: " + managerId));

        LocalDateTime now = LocalDateTime.now(); // 현재 시간
        List<Reservation> reservations;

        // 3. status 값에 따라 다른 쿼리 메소드 호출
        if ("upcoming".equalsIgnoreCase(status)) {
            reservations = reservationRepository.findUpcomingReservationsByUserAndManager(user, manager, now);
        } else if ("past".equalsIgnoreCase(status)) {
            reservations = reservationRepository.findPastReservationsByUserAndManager(user, manager, now);
        } else {
            throw new IllegalArgumentException("status 파라미터는 'upcoming' 또는 'past' 여야 합니다.");
        }

        // 4. 조회된 Reservation 리스트를 DTO 리스트로 변환하여 반환
        return reservations.stream()
                .map(MyReservationItemDto::new) // DTO 생성자 사용
                .collect(Collectors.toList());
    }

    public MobileTicketResponse getMobileTicket(Long reservationId, Authentication authentication) {

        // 1. 예매 정보 조회 (Fetch Join 활용)
        Reservation reservation = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다. ID: " + reservationId));

        // 2. 접근 권한 확인 (로그인한 사용자의 예매인지)
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");
        if (!reservation.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("자신의 예매 내역만 조회할 수 있습니다.");
        }

        // 3. DTO로 변환하여 반환
        return new MobileTicketResponse(reservation);
    }
}
