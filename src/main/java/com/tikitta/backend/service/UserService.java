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
import java.util.Map;
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
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매니저입니다. ID: " + managerId));

        List<Shows> showsList = showsRepository.findByManager(manager).stream()
                .filter(show -> show.getStatus() == DomainEnums.ShowStatus.PUBLISHED)
                .collect(Collectors.toList());

        List<ShowItemDto> showItemList = showsList.stream()
                .map(ShowItemDto::new)
                .collect(Collectors.toList());

        return new ShowListResponse(manager, showItemList);
    }

    public ShowDetailResponse getShowDetail(Long showId){
        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다. ID: " + showId));
        return new ShowDetailResponse(show);
    }

    public ManagerOrgResponse getManagerOrg(Long managerId){
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매니저입니다. ID: " + managerId));

        return new ManagerOrgResponse(manager);
    }

    public List<MyReservationItemDto> getMyReservations(Long managerId, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        KakaoOauth user = kakaoOauthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 사용자 정보를 찾을 수 없습니다."));

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매니저 ID입니다: " + managerId));

        LocalDateTime now = LocalDateTime.now();
        List<Reservation> reservations;


        reservations = reservationRepository.findUpcomingReservationsByUserAndManager(user, manager, now);

        return reservations.stream()
                .map(MyReservationItemDto::new)
                .collect(Collectors.toList());
    }

    public MobileTicketResponse getMobileTicket(Long reservationId, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        Reservation reservation = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다. ID: " + reservationId));

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        if (!reservation.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("자신의 예매 내역만 조회할 수 있습니다.");
        }

        return new MobileTicketResponse(reservation);
    }

    public ShowListResponse getUserMainPageWithReservationStatus(Long managerId, String userEmail) {

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매니저입니다. ID: " + managerId));

        List<Shows> showsList = showsRepository.findByManager(manager).stream()
                .filter(show -> show.getStatus() == DomainEnums.ShowStatus.PUBLISHED) // 여기 추가!
                .collect(Collectors.toList());

        List<Long> reservedShowIds = reservationRepository.findReservedShowIdsByUserEmail(userEmail);

        List<ShowItemDto> showItemList = showsList.stream()
                .map(show -> new ShowItemDto(show, reservedShowIds.contains(show.getId())))
                .collect(Collectors.toList());

        return new ShowListResponse(manager, showItemList);    }
}
