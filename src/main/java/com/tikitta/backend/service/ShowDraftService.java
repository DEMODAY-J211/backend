package com.tikitta.backend.service;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.ShowUpdateResponse;
import com.tikitta.backend.dto.ShowUpdateRequest;
import com.tikitta.backend.repository.*;
import com.tikitta.backend.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
            updateShowTimes(draft,request.getShowTimes());
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
    private void updateShowTimes(Shows draft, List<ShowUpdateRequest.ShowTimeInfo> items){
        draft.getShowTimes().clear();

        Location location = draft.getLocation();

        //공연장이 지정되어 있으면 좌석 복제 준비
        List<Seat> seats = null;
        boolean isSeated = false;

        if(location != null && location.getType() == DomainEnums.LocationType.SEATED){
            seats = seatRepository.findByLocation(location);
            isSeated = (seats != null && !seats.isEmpty());
        }

        List<ShowSeat> newBatch = new ArrayList<>();

        //회차 다시 생성
        for (var dto: items){
            LocalDateTime bookingEndAt = dto.getShowStart().minusHours(1);
            ShowTime st = ShowTime.builder()
                    .show(draft)
                    .startAt(dto.getShowStart())
                    .endAt(dto.getShowEnd())
                    .bookingEndAt(bookingEndAt)
                    .build();

            draft.getShowTimes().add(st);

            if(isSeated){
                //공연장 좌석이 있으면 해당 회차 좌석 생성
                if(seats != null){
                    for(Seat s: seats){
                        newBatch.add(ShowSeat.builder()
                                .showTime(st)
                                .seat(s)
                                .isAvailable(true)
                                .build());
                    }
                }
            }

            if (!newBatch.isEmpty()) {
                showSeatRepository.saveAll(newBatch);
            }
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

        if(dto.getReviewUrl() != null){
            message.setReviewUrl(dto.getReviewUrl());
        }

        messageRepository.save(message);
    }


}
