package com.tikitta.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.*;
import com.tikitta.backend.dto.ReservationDetailDto;
import com.tikitta.backend.repository.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.tikitta.backend.util.AuthUtil;
import com.tikitta.backend.util.SmsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // StringUtils import

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowService {

    private final AuthUtil authUtil;
    private final ManagerRepository managerRepository;
    private final ShowsRepository showsRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ShowTimeRepository showTimeRepository;
    private final ShowSeatRepository showSeatRepository;
    private final MessageRepository messageRepository;
    private final LocationMapRepository locationMapRepository;
    private final SmsUtil smsUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public DraftShowIdResponseDto getDraftShowId() {
        KakaoOauth user = authUtil.getCurrentUser();
        Manager manager = managerRepository.findByKakaoOauth(user)
            .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        Optional<Shows> draftShow = showsRepository.findTopByManagerAndStatusOrderByIdDesc(manager, DomainEnums.ShowStatus.DRAFT);

        Long showId = draftShow.map(Shows::getId).orElse(null);

        return new DraftShowIdResponseDto(showId);
    }

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

//        if (!show.getManager().getId().equals(manager.getId())) {
//            throw new AccessDeniedException("해당 공연에 대한 접근 권한이 없습니다.");
//        }

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
                .map(reservation -> {
                    boolean isReserved = reservation.getStatus() == DomainEnums.ReservationStatus.PENDING_PAYMENT ||
                            reservation.getStatus() == DomainEnums.ReservationStatus.CONFIRMED;
                    return new ReservationDetailDto(
                            reservation.getId(),
                            reservation.getShowTime().getId(),
                            reservation.getUser().getId(),
                            reservation.getReservationNumber(),
                            reservation.getUser().getName(),
                            reservation.getPhone(),
                            reservation.getCreatedAt(),
                            convertStatusToString(reservation.getStatus()), // 한글로 변환
                            isReserved,
                            new TicketDetailDto(reservation.getTicketOption(), reservation.getQuantity())
                    );
                })
                .collect(Collectors.toList());

        // 6. 최종 응답 DTO 생성 및 반환
        return new CustomerListResponseDto(
                show.getTitle(),
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

                DomainEnums.ReservationStatus originalStatus = reservation.getStatus();
                DomainEnums.ReservationStatus newStatus = convertStatus(info.getStatus());

                // 상태가 실제로 변경될 때만 업데이트 수행
                if (originalStatus != newStatus) {
                    reservation.setStatus(newStatus);
                    reservationItemRepository.updateStatusByReservationId(reservation.getId(), newStatus);

                    // 입금확정 상태로 변경 시, 예매 확정 메시지 발송
                    if (originalStatus == DomainEnums.ReservationStatus.PENDING_PAYMENT && newStatus == DomainEnums.ReservationStatus.CONFIRMED) {
                        sendBookingConfirmationMessage(reservation);
                    }
                }

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

    private void sendBookingConfirmationMessage(Reservation reservation) {
        try {
            Optional<Message> messageOpt = messageRepository.findByShow(reservation.getShowTime().getShow());
            if (messageOpt.isPresent()) {
                String template = messageOpt.get().getBookingConfirmation();
                if (StringUtils.hasText(template)) {
                    String finalMessage = SmsUtil.formatMessage(template, reservation);
                    //String subject = String.format("[%s] 예매가 확정되었습니다.", reservation.getShowTime().getShow().getTitle());
                    String subject = "[티킷타] 입금 확정안내";
                    smsUtil.sendLms(reservation.getPhone(), subject, finalMessage);
                    log.info("예매 확정 메시지 발송 완료: Reservation ID {}", reservation.getId());
                } else {
                    log.warn("예매 확정 메시지 템플릿이 비어있습니다. Reservation ID: {}", reservation.getId());
                }
            } else {
                log.warn("메시지 템플릿이 존재하지 않아 예매 확정 메시지를 발송할 수 없습니다. Show ID: {}", reservation.getShowTime().getShow().getId());
            }
        } catch (Exception e) {
            log.error("예매 확정 메시지 발송 중 오류 발생. Reservation ID: {}", reservation.getId(), e);
        }
    }


    private DomainEnums.ReservationStatus convertStatus(String status) {
        switch (status) {
            case "입금확정":
                return DomainEnums.ReservationStatus.CONFIRMED;
            case "환불대기":
                return DomainEnums.ReservationStatus.CANCEL_REQUESTED;
            case "취소완료":
                return DomainEnums.ReservationStatus.CANCELED;
            default:
                throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    private String convertStatusToString(DomainEnums.ReservationStatus status) {
        switch (status) {
            case PENDING_PAYMENT:
                return "입금대기";
            case CONFIRMED:
                return "입금확정";
            case CANCEL_REQUESTED:
                return "환불대기";
            case CANCELED:
                return "취소완료";
            default:
                return status.name();
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

//        if (!showTime.getShow().getManager().getId().equals(manager.getId())) {
//            throw new AccessDeniedException("해당 공연에 대한 접근 권한이 없습니다.");
//        }

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
                                    .phone(item.getReservation().getPhone())
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
//        if (!show.getManager().getId().equals(manager.getId())) {
//            throw new AccessDeniedException("해당 공연에 대한 접근 권한이 없습니다.");
//        }

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
                    if (show.getTicketOptions().isEmpty()) {
                        throw new IllegalStateException("해당 공연에 티켓 옵션이 없습니다.");
                    }
                    TicketOption ticketOption = show.getTicketOptions().get(0);

                    //새로운 reservation 생성
                    Reservation newReservation = Reservation.builder()
                            .reservationNumber(UUID.randomUUID().toString())
                            .user(manager.getKakaoOauth())
                            .showTime(showTime)
                            .ticketOption(ticketOption)
                            .quantity(1)
                            .totalPrice(ticketOption.getPrice())
                            .refundAccountNumber("현장 예매")
                            .phone("000-0000-0000")
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
    @Transactional(readOnly = true)
    public CheckinResponse getReservationSeatList(Long showId, Long showtimeId, String keyword) {
        KakaoOauth user = authUtil.getCurrentUser();
        Manager manager = managerRepository.findByKakaoOauth(user)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다."));

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

        // 예매 좌석 목록 조회
        List<ReservationItem> reservationItems;
        if (StringUtils.hasText(keyword)) {
            reservationItems = reservationItemRepository.findReservationItemsByShowTimeAndKeyword(selectedShowTime, keyword);
        } else {
            reservationItems = reservationItemRepository.findReservationItemsByShowTime(selectedShowTime);
        }

        List<ReservationSeatListResponse> reservationList = reservationItems.stream()
                .map(ri -> ReservationSeatListResponse.builder()
                        .reservationItemId(ri.getId())
                        .reservationId(ri.getReservation().getId())
                        .userId(ri.getReservation().getUser().getId())
                        .userName(ri.getReservation().getUser().getName())
                        .phone(ri.getReservation().getPhone())
                        .seat(ri.getShowSeat() != null ? ri.getShowSeat().getSeat().getSeatNumber() : null)
                        .ticketOptionId(ri.getReservation().getTicketOption().getId())
                        .isEntered(ri.isEntered())
                        .isReserved(ri.getReservation() != null &&
                                ri.getReservation().getStatus() != DomainEnums.ReservationStatus.CANCELED)
                        .reservationTime(ri.getReservation().getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // 좌석 배치도 생성
        List<List<Object>> seatMap = null;
        if (show.getSaleMethod() != DomainEnums.SaleMethod.STANDING) {
            try {
                LocationMap locationMap = locationMapRepository.findByLocationId(show.getLocation().getId())
                        .orElseThrow(() -> new IllegalStateException("해당 공연장의 좌석 배치도 정보를 찾을 수 없습니다."));

                int height = locationMap.getLayoutHeight();
                int width = locationMap.getLayoutWidth();
                seatMap = new ArrayList<>();
                for (int i = 0; i < height; i++) {
                    seatMap.add(new ArrayList<>(Collections.nCopies(width, 0)));
                }

                List<List<Integer>> stageCoords = objectMapper.readValue(locationMap.getStageCoordinates(), new TypeReference<>() {});
                for (List<Integer> coord : stageCoords) {
                    seatMap.get(coord.get(0)).set(coord.get(1), -1);
                }

                List<ShowSeat> showSeats = showSeatRepository.findByShowTime(selectedShowTime);
                for (ShowSeat showSeat : showSeats) {
                    Seat seat = showSeat.getSeat();
                    seatMap.get(seat.getSeatRow()).set(seat.getSeatColumn(), seat.getSeatNumber());
                }
            } catch (IOException e) {
                throw new RuntimeException("좌석 배치도 데이터를 처리하는 중 오류가 발생했습니다.", e);
            }
        }

        return CheckinResponse.builder()
                .seat(seatMap)
                .reservation(reservationList)
                .build();
    }

    public ShowDraftResponse getPublishShow(Long showId){
        KakaoOauth user=authUtil.getCurrentUser();
        Manager manager=managerRepository.findByKakaoOauth(user)
                .orElseThrow(()->new IllegalArgumentException("해당 매니저 정보를 찾을 수 없습니다."));

        Shows draft = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공연이 존재하지 않습니다."));

        if (draft.getStatus() != DomainEnums.ShowStatus.PUBLISHED) {
            throw new IllegalStateException("최종 상태의 공연만 조회할 수 있습니다.");
        }

        Message message = messageRepository.findByShow(draft).orElse(null);

        // 1. Location 객체 null-safe하게 가져오기
        Location location = draft.getLocation();

        // 2. 중첩 DTO: ShowTimeInfo 리스트 매핑
        List<ShowDraftResponse.ShowTimeInfo> showTimeInfos = draft.getShowTimes().stream()
                .map(st -> ShowDraftResponse.ShowTimeInfo.builder()
                        .showStart(st.getStartAt())
                        .showEnd(st.getEndAt())
                        .build())
                .collect(Collectors.toList());

        // 3. 중첩 DTO: TicketOptionInfo 리스트 매핑
        List<ShowDraftResponse.TicketOptionInfo> ticketOptionInfos = draft.getTicketOptions().stream()
                .map(opt -> ShowDraftResponse.TicketOptionInfo.builder()
                        .name(opt.getName())
                        .description(opt.getDescription())
                        .price(opt.getPrice())
                        .build())
                .collect(Collectors.toList());

        // 4. 중첩 DTO: ShowMessageInfo 매핑
        ShowDraftResponse.ShowMessageInfo messageInfo = null;
        if (message != null) {
            messageInfo = ShowDraftResponse.ShowMessageInfo.builder()
                    .payGuide(message.getPaymentGuide())
                    .bookConfirm(message.getBookingConfirmation())
                    .showGuide(message.getShowGuide())
                    .reviewRequest(message.getReviewRequest())
                    .reviewUrl(draft.getReviewUrl()) // (엔티티의 reviewUrl을 Message DTO에 매핑)
                    .build();
        }

        // 5. 최종 ShowDraftResponse DTO 빌드 및 반환
        return ShowDraftResponse.builder()
                .title(draft.getTitle())
                .poster(draft.getPosterUrl())
                .showTimes(showTimeInfos)
                .bookStart(draft.getBookingStartAt())
                .ticketOptions(ticketOptionInfos)
                .bankMaster(draft.getBankDepositorName())
                .bankName(draft.getBankName().name())
                .bankAccount(draft.getBankAccountNumber())
                .detailImages(draft.getDetailImageUrls())
                .detailText(draft.getDetailText())
                .locationId(location.getId())
                .locationName(location.getName())
                .seatType(draft.getSaleMethod().name())
                .seatCount(Math.toIntExact(draft.getSeatCount()))
                .showMessage(messageInfo)
                .status(draft.getStatus().name())
                .reviewUrl(draft.getReviewUrl()) // (DTO 최상위에 reviewUrl 필드가 있다고 가정)
                .build();
    }

    public ShowUpdateResponse updatePublishedShow(Long showId, ShowPublishUpdateRequest request){

        KakaoOauth user=authUtil.getCurrentUser();
        Manager manager= managerRepository.findByKakaoOauth(user)
                .orElseThrow(()->new IllegalArgumentException("해당 매니저의 정보를 찾을 수 없습니다."));

        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 ID입니다: " + showId));

        if (show.getStatus() != DomainEnums.ShowStatus.PUBLISHED) {
            throw new IllegalStateException("PUBLISHED 상태의 공연만 수정할 수 있습니다.");
        }

        if (request.getDetailImages() != null) {
            show.getDetailImageUrls().clear();
            show.getDetailImageUrls().addAll(request.getDetailImages());
        }

        if (request.getDetailText() != null) {
            show.setDetailText(request.getDetailText());
        }

        return new ShowUpdateResponse(show.getId(), show.getStatus().name());
    }



}
