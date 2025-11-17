package com.tikitta.backend.service;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.ShowDraftDeleteResponse;
import com.tikitta.backend.dto.ShowDraftResponse;
import com.tikitta.backend.dto.ShowUpdateResponse;
import com.tikitta.backend.dto.ShowUpdateRequest;
import com.tikitta.backend.repository.*;
import com.tikitta.backend.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowDraftService {

    private final ShowsRepository showsRepository;
    private final ManagerRepository managerRepository;
    private final AuthUtil authUtil;
    private final LocationRepository locationRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatRepository seatRepository;
    private final MessageRepository messageRepository;
    private final RestClient.Builder builder;

    public ShowUpdateResponse CreateShow(){
        KakaoOauth user=authUtil.getCurrentUser();

        Manager manager=managerRepository.findByKakaoOauth(user)
                .orElseThrow(()->new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        Shows show = Shows.builder()
                .manager(manager)
                .status(DomainEnums.ShowStatus.DRAFT)
                .build();

        Shows saved = showsRepository.save(show);

        return  ShowUpdateResponse.builder()
                .showId(saved.getId())
                .status(saved.getStatus().name())
                .build();
    }

    public ShowUpdateResponse updateShow(Long showId, ShowUpdateRequest request){

        KakaoOauth user=authUtil.getCurrentUser();
        Manager manager=managerRepository.findByKakaoOauth(user)
                .orElseThrow(()->new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        Shows draft = showsRepository.findById(showId)
                .orElseThrow(()->new IllegalArgumentException("해당 공연이 존재하지 않습니다."));

        if(!draft.getStatus().equals(DomainEnums.ShowStatus.DRAFT)){
            throw new IllegalArgumentException("임시저장 상태의 공연만 수정할 수 있습니다.");
        }

        updateBasicFields(draft, request);

        if(request.getLocationId() != null && (draft.getLocation() == null || !draft.getLocation().getId().equals(request.getLocationId()))){
            updateLocationAndCloneSeats(draft,request.getLocationId());
        }

        if(request.getShowTimes() != null){
            updateShowTimes(draft,request);
        }
        /* [D] 티켓 옵션 교체 */
        if (request.getTicketOptions() != null) {
            updateTicketOptions(draft, request.getTicketOptions());
        }

        /* [E] 상세 이미지 교체 */
        if (request.getDetailImages() != null) {
            draft.getDetailImageUrls().clear();
            draft.getDetailImageUrls().addAll(request.getDetailImages());
        }

        /* [F] Message 수정 */
        if (request.getShowMessage() != null) {
            updateMessage(draft, request.getShowMessage());
        }

        return new ShowUpdateResponse(draft.getId(), draft.getStatus().name());
    }

    private void updateBasicFields(Shows draft, ShowUpdateRequest request){
        if(request.getTitle()!= null) draft.setTitle(request.getTitle());
        if(request.getPoster() != null) draft.setPosterUrl(request.getPoster());
        if(request.getBookStart() != null) draft.setBookingStartAt(request.getBookStart());
        if(request.getBankMaster() != null) draft.setBankDepositorName(request.getBankMaster());
        if (request.getBankName() != null)
            draft.setBankName(DomainEnums.Bank.valueOf(request.getBankName()));
        if(request.getBankAccount()!=null) draft.setBankAccountNumber(request.getBankAccount());
        if(request.getDetailText() != null) draft.setDetailText(request.getDetailText());
        if (request.getSeatType() != null)
            draft.setSaleMethod(DomainEnums.SaleMethod.valueOf(request.getSeatType()));
        if (request.getSeatCount() != null) {
            draft.setSeatCount(Long.valueOf(request.getSeatCount()));
            List<ShowTime> showTimes = draft.getShowTimes();
            for (ShowTime st : showTimes) {
                st.setRemainSeatCount(Long.valueOf(request.getSeatCount()));
            }
        }

        if (request.getReviewUrl() != null) draft.setReviewUrl(request.getReviewUrl());

    }

    //공연장 변경 처리, 모든 회차 좌석 복제
    private void updateLocationAndCloneSeats(Shows draft, Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연장입니다."));

        draft.setLocation(location);

        //기존 좌석 전체 삭제
        showSeatRepository.deleteByShow(draft);

        if (location.getType() == DomainEnums.LocationType.SEATED) {

            //공연장의 모든 좌석 로드
            List<Seat> seats = seatRepository.findByLocation(location);

            //회차가 없으면 복제 필요없음
            if (draft.getShowTimes().isEmpty()) return;

            //모든 기존 회차에 대해 좌석 복제 -> 판매가능한 좌석
            List<ShowSeat> batch = new ArrayList<>();

            for (ShowTime st : draft.getShowTimes()) {
                for (Seat s : seats) {
                    batch.add(ShowSeat.builder()
                            .showTime(st)
                            .seat(s)
                            .isAvailable(true)
                            .build());
                }
            }
            showSeatRepository.saveAll(batch);
        }
    }

    //회차 변경 시 -> 해당 회차만 좌석 복제
    private void updateShowTimes(Shows draft, ShowUpdateRequest request) {

        draft.getShowTimes().clear(); // 기존 회차/좌석은 orphanRemoval로 모두 삭제

        Location location = draft.getLocation();
        List<Seat> seats = null;
        boolean isSeated = false;

        if (location != null && location.getType() == DomainEnums.LocationType.SEATED) {
            seats = seatRepository.findByLocation(location);
            isSeated = (seats != null && !seats.isEmpty() && draft.getSaleMethod() != DomainEnums.SaleMethod.STANDING);
        }

        List<ShowSeat> newBatch = new ArrayList<>();


        List<ShowUpdateRequest.ShowTimeInfo> items = request.getShowTimes();

        // 회차 다시 생성
        for (var dto : items) {
            LocalDateTime bookingEndAt = dto.getShowStart().minusHours(1);

            // 1. 빌더 준비 (공통 정보)
            ShowTime.ShowTimeBuilder builder = ShowTime.builder()
                    .show(draft)
                    .startAt(dto.getShowStart())
                    .endAt(dto.getShowEnd())
                    .bookingEndAt(bookingEndAt);

            // 2. 스탠딩(!isSeated)일 때만 총수량을 빌더에 추가
            if (!isSeated) {
                builder.totalStandingQuantity(request.getSeatCount());
            }

            // 3. ShowTime 객체를 먼저 *완성*시킴
            ShowTime st = builder.build();
            draft.getShowTimes().add(st);

            // 4. 좌석제(isSeated)일 때, 완성된 'st'를 사용해 newBatch에 좌석 추가
            if (isSeated) {
                if (seats != null) {
                    for (Seat s : seats) {
                        newBatch.add(ShowSeat.builder()
                                .showTime(st)
                                .seat(s)
                                .isAvailable(true)
                                .build());
                    }
                }
            }
        } // <-- for 루프 종료

        // 5. 루프가 다 끝난 후 모아둔 좌석을 한 번에 저장
        if (!newBatch.isEmpty()) {
            showSeatRepository.saveAll(newBatch);
        }
    }

    private void updateTicketOptions(Shows draft, List<ShowUpdateRequest.TicketOptionInfo> items){
        draft.getTicketOptions().clear();

        for(var dto:items){
            TicketOption option = TicketOption.builder()
                    .show(draft)
                    .name(dto.getName())
                    .description(dto.getDecription())
                    .price(dto.getPrice())
                    .build();

            draft.getTicketOptions().add(option);
        }
    }

    private void updateMessage(Shows draft, ShowUpdateRequest.ShowMessageInfo dto){
        Message message = messageRepository.findByShow(draft)
                .orElse(Message.builder().show(draft).build());

        if (dto.getPayGuide() != null) {
            message.setPaymentGuide(dto.getPayGuide());
        }
        if (dto.getBookConfirm() != null) {
            message.setBookingConfirmation(dto.getBookConfirm());
        }
        if (dto.getShowGuide() != null) {
            message.setShowGuide(dto.getShowGuide());
        }

        if(dto.getReviewRequest() != null){
            message.setReviewRequest(dto.getReviewRequest());
        }

        if (dto.getReviewUrl() != null) {
            draft.setReviewUrl(dto.getReviewUrl());
        }

        messageRepository.save(message);
    }

    public ShowUpdateResponse publishShow(Long showId){
        KakaoOauth user=authUtil.getCurrentUser();
        Manager manager = managerRepository.findByKakaoOauth(user)
                .orElseThrow(()->new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        Shows draft = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공연이 존재하지 않습니다."));

        if (draft.getStatus() != DomainEnums.ShowStatus.DRAFT) {
            throw new IllegalStateException("임시저장 상태의 공연만 최종 등록할 수 있습니다.");
        }

        vaildateShowForPublishing(draft);

        draft.setStatus(DomainEnums.ShowStatus.PUBLISHED);
        return new ShowUpdateResponse(draft.getId(), draft.getStatus().name());
    }

    private void vaildateShowForPublishing(Shows draft) {
        if (draft.getTitle() == null || draft.getTitle().isBlank())
            throw new IllegalStateException("공연 제목을 입력해야 합니다.");

//        if (draft.getPosterUrl() == null || draft.getPosterUrl().isBlank())
//            throw new IllegalStateException("포스터를 등록해야 합니다.");

        if (draft.getBookingStartAt() == null)
            throw new IllegalStateException("예매 시작일을 입력해야 합니다.");

        if (draft.getBankName() == null || draft.getBankAccountNumber() == null)
            throw new IllegalStateException("정산 계좌 정보를 모두 입력해야 합니다.");

        if (draft.getLocation() == null)
            throw new IllegalStateException("공연장을 선택해야 합니다.");

        List<ShowTime> showTimes = draft.getShowTimes();
        if (showTimes == null || showTimes.isEmpty())
            throw new IllegalStateException("공연 회차를 1개 이상 등록해야 합니다.");

        // 좌석 vs 스탠딩

        if (draft.getLocation().getType() == DomainEnums.LocationType.SEATED) {
            // 좌석 공연이라면 showSeat가 존재해야 한다.
            if (!showSeatRepository.existsByShowTime_Show(draft)) {
                throw new IllegalStateException("좌석제 공연이지만 좌석 정보가 생성되지 않았습니다.");
            }
        } else {
            // 스탠딩 수량 검증
            int standingTotal = showTimes.stream()
                    .mapToInt(st -> st.getTotalStandingQuantity() == null ? 0 : st.getTotalStandingQuantity())
                    .sum();
            if (standingTotal <= 0)
                throw new IllegalStateException("스탠딩 공연의 스탠딩 수량을 입력해야 합니다.");
        }

        // --- 티켓 옵션 ---
        if (draft.getTicketOptions() == null || draft.getTicketOptions().isEmpty())
            throw new IllegalStateException("티켓 옵션을 1개 이상 등록해야 합니다.");

        // --- 메시지 ---
        Message msg = messageRepository.findByShow(draft)
                .orElseThrow(() -> new IllegalStateException("메시지 정보를 입력해야 합니다."));

        if (msg.getPaymentGuide() == null || msg.getPaymentGuide().isBlank()
                || msg.getBookingConfirmation() == null || msg.getBookingConfirmation().isBlank()
                || msg.getShowGuide() == null || msg.getShowGuide().isBlank()) {
            throw new IllegalStateException("입금 안내, 예매 확정, 공연 안내 메시지는 필수입니다.");
        }
    }

    public ShowDraftResponse getShowDraft(Long showId){
        KakaoOauth user=authUtil.getCurrentUser();
        Manager manager=managerRepository.findByKakaoOauth(user)
                .orElseThrow(()->new IllegalArgumentException("해당 매니저 정보를 찾을 수 없습니다."));

        Shows draft = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공연이 존재하지 않습니다."));

        if (draft.getStatus() != DomainEnums.ShowStatus.DRAFT) {
            throw new IllegalStateException("임시저장 상태의 공연만 조회할 수 있습니다.");
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
                .locationId(location != null ? location.getId() : null)
                .locationName(location != null ? location.getName() : null)
                .seatType(draft.getSaleMethod() != null ? draft.getSaleMethod().name() : null)
                .seatCount(Math.toIntExact(draft.getSeatCount()))
                .showMessage(messageInfo)
                .status(draft.getStatus().name())
                .reviewUrl(draft.getReviewUrl()) // (DTO 최상위에 reviewUrl 필드가 있다고 가정)
                .build();
    }

    public ShowDraftDeleteResponse deleteShowDraft() {

        KakaoOauth user = authUtil.getCurrentUser();
        Manager manager = managerRepository.findByKakaoOauth(user)
                .orElseThrow(() -> new IllegalArgumentException("매니저 정보를 찾을 수 없습니다."));

        Shows draft = showsRepository.findByManagerAndStatus(manager, DomainEnums.ShowStatus.DRAFT)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 임시저장 공연이 없습니다."));

        // 3. 공연 삭제
        // (Shows에 Cascade/orphanRemoval이 잘 설정되어 있다면
        //  이 한 줄이 ShowTime, TicketOption, ShowSeat, Message 등을 연쇄적으로 삭제합니다)
        showsRepository.delete(draft);

        // 4. API 명세에 따라 { "deletedCount": 1 } 반환
        return new ShowDraftDeleteResponse(1);
    }
}
