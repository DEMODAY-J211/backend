package com.tikitta.backend.service;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.*;
import com.tikitta.backend.repository.*;
import java.util.ArrayList;
import java.util.List;
import com.tikitta.backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // StringUtils import

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowService {

    private final AuthUtil authUtil;
    private final ManagerRepository managerRepository;
    private final KakaoOauthRepository kakaoOauthRepository;
    private final ShowsRepository showsRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ShowTimeRepository showTimeRepository;
    private final ShowSeatRepository showSeatRepository;

    // ... (기존 getMyShows, getReservationList 메소드)
    public MyShowListResponseDto getMyShows() {
        KakaoOauth user=authUtil.getCurrentUser();

        Manager manager = managerRepository.findByKakaoOauth(user)
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

    public CustomerListResponseDto getReservationList(Long showId, Long showtimeId) {
        // 이 메소드는 이제 searchReservationList로 대체될 수 있습니다.
        return searchReservationList(showId, showtimeId, null);
    }


    // ▼▼▼ 새로 추가된 검색 메소드 ▼▼▼
    public CustomerListResponseDto searchReservationList(Long showId, Long showtimeId, String keyword) {
        // 1. 매니저 인증 및 공연 소유권 확인
        KakaoOauth user=authUtil.getCurrentUser();

        Manager manager = managerRepository.findByKakaoOauth(user)
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
            selectedShowTime = allShowTimes.get(0);
        }

        // 4. 예매 목록 조회 (keyword 유무에 따라 분기)
        List<Reservation> reservations;
        if (StringUtils.hasText(keyword)) {
            // 키워드가 있으면 검색 쿼리 실행
            reservations = reservationRepository.findByShowTimeAndKeywordWithDetails(selectedShowTime, keyword);
        } else {
            // 키워드가 없으면 전체 목록 조회
            reservations = reservationRepository.findByShowTimeWithDetailsOrderByCreatedAtDesc(selectedShowTime);
        }

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
                keyword, // 검색 키워드 포함
                reservationDetailDtoList
        );
    }

    @Transactional
    public ReservationStatusUpdateResponse updateReservationStatus(Long showId, Long showtimeId, ReservationStatusUpdateRequest request) {
        int updatedCount = 0;
        List<Long> failedIds = new ArrayList<>();

        for (ReservationStatusInfo info : request.getReservations()) {
            try {
                Reservation reservation = reservationRepository.findById(info.getReservationId())
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

                if (!reservation.getShowTime().getShow().getId().equals(showId)) {
                    throw new AccessDeniedException("Reservation does not belong to the specified show.");
                }

                DomainEnums.ReservationStatus newStatus = convertStatus(info.getStatus());

                // Update Reservation status
                reservationRepository.updateStatus(reservation.getId(), newStatus);

                // Update ReservationItem status
                reservationItemRepository.updateStatusByReservationId(reservation.getId(), newStatus);

                if (newStatus == DomainEnums.ReservationStatus.CANCELED) {
                    // Restore ShowTime seat count
                    showTimeRepository.increaseRemainSeat(reservation.getShowTime().getId(), reservation.getQuantity());

                    // Restore ShowSeat availability
                    List<ReservationItem> items = reservationItemRepository.findByReservation(reservation);
                    for (ReservationItem item : items) {
                        if (item.getShowSeat() != null) {
                            showSeatRepository.updateIsAvailable(item.getShowSeat().getId(), true);
                        }
                    }
                }

                updatedCount++;
            } catch (Exception e) {
                failedIds.add(info.getReservationId());
            }
        }

        return new ReservationStatusUpdateResponse(updatedCount, failedIds);
    }

    private DomainEnums.ReservationStatus convertStatus(String status) {
        switch (status) {
            case "입금확인":
                return DomainEnums.ReservationStatus.CONFIRMED;
            case "환불대기":
                return DomainEnums.ReservationStatus.CANCEL_REQUESTED;
            case "환불완료":
                return DomainEnums.ReservationStatus.CANCELED;
            default:
                throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    //좌석별 조회
    @Transactional(readOnly = true)
    public List<ReservationSeatListResponse> getReservationSeatList(Long showtimeId) {
        // 1. 매니저 인증
        KakaoOauth user=authUtil.getCurrentUser();

        // 2. 매니저 조회
        Manager manager = managerRepository.findByKakaoOauth(user)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        // 3. 공연 소유권 체크
        ShowTime showTime = showTimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차 ID입니다: " + showtimeId));

        if (!showTime.getShow().getManager().getId().equals(manager.getId())) {
            throw new AccessDeniedException("해당 공연에 대한 접근 권한이 없습니다.");
        }

        //해당 회차의 모든 예약을 조회
        List<Reservation> reservations=reservationRepository.findByShowTime(showTime);

        //Dto 변환
        /*Dto에 이 코드를 넣는 것이 나을까...*/
        return reservations.stream()
                .flatMap(reservation -> reservation.getReservationItems().stream()
                        .map(item -> {
                            boolean reserved = item.getReservation().getStatus() == DomainEnums.ReservationStatus.CONFIRMED; //예약 확정에 대해서만 true
                            String seatLabel = (item.getShowSeat() != null && item.getShowSeat().getSeat() != null)
                                    ? item.getShowSeat().getSeat().getSeatNumber()
                                    : null; //스탠딩일때 좌석 null 반환

                            return ReservationSeatListResponse.builder()
                                    .reservationItemId(item.getId())
                                    .reservationId(item.getReservation().getId())
                                    .userId(item.getReservation().getUser().getId())
                                    .userName(item.getReservation().getUser().getName())
                                    .phone(item.getReservation().getUser().getPhone())
                                    .seat(seatLabel)
                                    .ticketOptionId(item.getReservation().getTicketOption().getId())
                                    .isEntered(item.isEntered())
                                    .isReserved(reserved)
                                    .reservationTime(item.getReservation().getCreatedAt())
                                    .build();
                        })
                )
                .collect(Collectors.toList());
    }

    @Transactional
    public CheckinStatusUpdateResponse updateCheckinStatus(Long showId, Long showtimeId, CheckinStatusUpdateRequest request){
        KakaoOauth user=authUtil.getCurrentUser();

        // 매니저 조회
        Manager manager = managerRepository.findByKakaoOauth(user)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다."));
        if (!show.getManager().getId().equals(manager.getId())) {
            throw new AccessDeniedException("해당 공연에 대한 접근 권한이 없습니다.");
        }

        //회차 검증
        ShowTime showTime = showTimeRepository.findById(showtimeId).orElseThrow(()-> new IllegalArgumentException("존재하지 않은 회차의 ID입니다."));

        if (!showTime.getShow().getId().equals(showId)) {
            throw new IllegalArgumentException("회차가 해당 공연에 속하지 않습니다.");
        }

        //좌석 상태 변경 로직 수행(isReserved, isEntered 수정)
        int updatedCount =0;
        List<Long> failedIds = new ArrayList<>();

        for (CheckinStatusUpdateRequest.CheckinStatusUpdateItem item :  request.getCheckinStatusUpdateRequest()){
            try {
                ReservationItem reservationItem = reservationItemRepository.findById(item.getReservationItemId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매 항목입니다."));

                //좌석 취소
                if (Boolean.FALSE.equals(item.getIsReserved())) {
                    reservationItem.setEntered(false);
                    reservationItem.getReservation().setStatus(DomainEnums.ReservationStatus.CANCELED);
                }

                //현장 예매 생성 & 입장 처리
                else if (Boolean.TRUE.equals(item.getIsReserved())&&item.getReservationItemId() == null) {

                    //새로운 reservation 생성
                    Reservation newReservation = Reservation.builder()
                            .user(manager.getKakaoOauth())
                            .showTime(showTime)
                            .status(DomainEnums.ReservationStatus.CONFIRMED)
                            .createdAt(LocalDateTime.now())
                            .build();
                    reservationRepository.save(newReservation);

                    //reservationItem 생성
                    ReservationItem newItem;
                    if(item.getShowSeatId()!=null) { //좌석제
                        ShowSeat showSeat = showSeatRepository.findById(item.getShowSeatId())
                                .orElseThrow(()-> new IllegalArgumentException("좌석 정보를 찾을 수 없습니다."));
                        newItem = ReservationItem.builder()
                                .reservation(newReservation)
                                .showSeat(showSeat)
                                .build();
                    }
                    else{//스탠딩
                        newItem=ReservationItem.builder()
                                .reservation(newReservation)
                                .entryNumber(item.getEntryNumber())
                                .build();
                    }
                    newItem.checkIn(); //입장 처리
                    reservationItemRepository.save(newItem);
                }

                //입장 상태만 수정
                else if(item.getIsEntered()!=null && item.getReservationItemId() !=null){
                    ReservationItem reservationItem2 = reservationItemRepository.findById(item.getReservationItemId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매 항목입니다."));
                    reservationItem2.setEntered(item.getIsEntered());
                }

                updatedCount++;

            } catch (Exception e){
                failedIds.add(item.getReservationItemId());
            }
        }

        return CheckinStatusUpdateResponse.builder()
                .updatedCount(updatedCount)
                .failedIds(failedIds)
                .build();
    }

    //좌석별 조회
    @Transactional
    public List<ReservationSeatListResponse> getReservationSeatList(Long showId, Long showtimeId, String keyword) {
        KakaoOauth user = authUtil.getCurrentUser();

        // 매니저 조회
        Manager manager = managerRepository.findByKakaoOauth(user)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다."));

        //회차 목록 조회 및 선택
        List<ShowTime> allShowTimes = show.getShowTimes().stream()
                .sorted(Comparator.comparing(ShowTime::getStartAt))
                .collect(Collectors.toList());

        if (allShowTimes.isEmpty()) {
            throw new IllegalArgumentException("해당 공연에 등록된 회차가 없습니다.");
        }

        ShowTime selectedShowTime;
        if (showtimeId != null) {
            selectedShowTime = allShowTimes.stream()
                    .filter(st -> st.getId().equals(showtimeId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차 ID입니다: " + showtimeId));
        } else {
            selectedShowTime = allShowTimes.get(0);
        }

        // 4. 예매 좌석 목록 조회 (이름, 좌석번호 기준 검색)
        List<ReservationItem> reservationItems;
        if (StringUtils.hasText(keyword)) {
            reservationItems = reservationItemRepository.findReservationItemsByShowTimeAndKeyword(selectedShowTime, keyword);
        } else {
            reservationItems = reservationItemRepository.findReservationItemsByShowTime(selectedShowTime);
        }

        // 5. DTO 변환
        List<ReservationSeatListResponse> responseList = reservationItems.stream()
                .map(ri -> ReservationSeatListResponse.builder()
                        .reservationItemId(ri.getId())
                        .reservationId(ri.getReservation().getId())
                        .userId(ri.getReservation().getUser().getId())
                        .userName(ri.getReservation().getUser().getName())
                        .phone(ri.getReservation().getUser().getPhone())
                        .seat(ri.getShowSeat() != null ? ri.getShowSeat().getSeat().getSeatNumber() : null)
                        .ticketOptionId(ri.getReservation().getTicketOption().getId())
                        .isEntered(ri.isEntered())
                        .isReserved(ri.getReservation() != null &&
                                ri.getReservation().getStatus() != DomainEnums.ReservationStatus.CANCEL_REQUESTED)
                        .reservationTime(ri.getReservation().getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // 6. 반환
        return responseList;

    }
}
