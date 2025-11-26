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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowDraftService {

    private final ShowsRepository showsRepository;
    private final ManagerRepository managerRepository;
    private final AuthUtil authUtil;
    private final LocationRepository locationRepository;
    private final ShowSeatRepository showSeatRepository;
    private final MessageRepository messageRepository;
    private final RestClient.Builder builder;
    private final ShowTimeRepository showTimeRepository;
    private final ImageService imageService;

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
            updateLocation(draft,request.getLocationId());
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
           updateDetailImages(draft,request);
        }

        /* [F] Message 수정 */
        if (request.getShowMessage() != null) {
            updateMessage(draft, request.getShowMessage());
        }

        return new ShowUpdateResponse(draft.getId(), draft.getStatus().name());
    }

    private void updateBasicFields(Shows draft, ShowUpdateRequest request){
        if(request.getTitle()!= null && !request.getTitle().isBlank()) draft.setTitle(request.getTitle());
        if(request.getPoster() != null && !request.getPoster().isBlank())
            updatePoster(draft, request.getPoster());
        if(request.getBookStart() != null) draft.setBookingStartAt(request.getBookStart());
        if(request.getBankMaster() != null && !request.getBankMaster().isBlank()) draft.setBankDepositorName(request.getBankMaster());
        if (request.getBankName() != null && !request.getBankName().isBlank())
            draft.setBankName(DomainEnums.Bank.valueOf(request.getBankName()));
        if(request.getBankAccount()!=null && !request.getBankAccount().isBlank()) draft.setBankAccountNumber(request.getBankAccount());
        if(request.getDetailText() != null && !request.getDetailText().isBlank()) draft.setDetailText(request.getDetailText());
        if (request.getSeatType() != null && !request.getSeatType().isBlank())
            draft.setSaleMethod(DomainEnums.SaleMethod.valueOf(request.getSeatType()));
        if (request.getSeatCount() != null) {
            draft.setSeatCount(request.getSeatCount());
            List<ShowTime> showTimes = draft.getShowTimes();
            for (ShowTime st : showTimes) {
                st.setRemainSeatCount(request.getSeatCount()); /*TODO: 이거 예매자 수로 바꾸기*/
            }
        }

        if (request.getReviewUrl() != null && !request.getReviewUrl().isBlank()) draft.setReviewUrl(request.getReviewUrl());

    }

    //공연장 변경 처리
    private void updateLocation(Shows draft, Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연장입니다."));
        draft.setLocation(location);
    }

    //회차 변경
    private void updateShowTimes(Shows draft, ShowUpdateRequest request) {
        draft.getShowTimes().clear(); // 기존 회차는 orphanRemoval로 모두 삭제

        List<ShowUpdateRequest.ShowTimeInfo> items = request.getShowTimes();

        // 회차 다시 생성
        for (var dto : items) {
            if (dto.getShowStart() == null || dto.getShowEnd() == null) {
                continue; // 필수 시간 정보가 없으면 건너뜁니다.
            }
            LocalDateTime bookingEndAt = dto.getShowStart().minusHours(1);

            Long seatCount = request.getSeatCount();
            if (seatCount == null) {
                seatCount = draft.getSeatCount();
            }

            if (seatCount == null) {
                seatCount = 100L;
                //throw new IllegalStateException("좌석 수 없이 회차를 생성/수정할 수 없습니다.");
            }

            ShowTime.ShowTimeBuilder builder = ShowTime.builder()
                    .show(draft)
                    .startAt(dto.getShowStart())
                    .endAt(dto.getShowEnd())
                    .bookingEndAt(bookingEndAt)
                    .remainSeatCount(seatCount);

            // 스탠딩 또는 주최자 배정일 때만 총수량을 빌더에 추가
            if (draft.getSaleMethod() == DomainEnums.SaleMethod.STANDING || draft.getSaleMethod() == DomainEnums.SaleMethod.EVENTHOST) {
                builder.totalStandingQuantity(seatCount);
            }

            ShowTime st = builder.build();
            draft.getShowTimes().add(st);
        }
    }

    private void updateTicketOptions(Shows draft, List<ShowUpdateRequest.TicketOptionInfo> items){
        draft.getTicketOptions().clear();

        for(var dto:items){
            if (dto.getName() != null && !dto.getName().isBlank() && dto.getPrice() != null) {
                TicketOption option = TicketOption.builder()
                        .show(draft)
                        .name(dto.getName())
                        .description(dto.getDescription())
                        .price(dto.getPrice())
                        .build();

                draft.getTicketOptions().add(option);
            }
        }
    }

    private void updateMessage(Shows draft, ShowUpdateRequest.ShowMessageInfo dto){
        Message message = messageRepository.findByShow(draft)
                .orElse(Message.builder().show(draft).build());

        if (dto.getPayGuide() != null && !dto.getPayGuide().isBlank()) {
            message.setPaymentGuide(dto.getPayGuide());
        }
        if (dto.getBookConfirm() != null && !dto.getBookConfirm().isBlank()) {
            message.setBookingConfirmation(dto.getBookConfirm());
        }
        if (dto.getShowGuide() != null && !dto.getShowGuide().isBlank()) {
            message.setShowGuide(dto.getShowGuide());
        }

        if(dto.getReviewRequest() != null && !dto.getReviewRequest().isBlank()){
            message.setReviewRequest(dto.getReviewRequest());
        }

        if (dto.getReviewUrl() != null && !dto.getReviewUrl().isBlank()) {
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

        // --- 좌석 복제 로직 ---
        if (draft.getSaleMethod() != DomainEnums.SaleMethod.STANDING && draft.getSaleMethod() != DomainEnums.SaleMethod.EVENTHOST) {
            cloneSeatsForAllShowTimes(draft);
        }
        
        draft.setStatus(DomainEnums.ShowStatus.PUBLISHED);
        return new ShowUpdateResponse(draft.getId(), draft.getStatus().name());
    }

    private void cloneSeatsForAllShowTimes(Shows draft) {
        Long locationId = draft.getLocation().getId();
        List<ShowSeat> seatTemplates = showSeatRepository.findBySeat_Location_IdAndShowTimeIsNull(locationId);
        
        if (seatTemplates.isEmpty()) {
            throw new IllegalStateException("좌석제 공연의 좌석 정보가 존재하지 않습니다. 좌석을 먼저 설정해주세요.");
        }

        List<ShowTime> showTimes = draft.getShowTimes();
        List<ShowSeat> newShowSeats = new ArrayList<>();

        for (ShowTime showTime : showTimes) {
            for (ShowSeat template : seatTemplates) {
                newShowSeats.add(ShowSeat.builder()
                        .showTime(showTime)
                        .seat(template.getSeat())
                        .isAvailable(true)
                        .isGoodSeat(template.getIsGoodSeat())
                        .build());
            }
        }

        showSeatRepository.saveAll(newShowSeats);
        showSeatRepository.deleteBySeat_Location_IdAndShowTimeIsNull(locationId);
    }

    private void vaildateShowForPublishing(Shows draft) {
        if (draft.getTitle() == null || draft.getTitle().isBlank())
            throw new IllegalStateException("공연 제목을 입력해야 합니다.");

        if (draft.getBookingStartAt() == null)
            throw new IllegalStateException("예매 시작일을 입력해야 합니다.");

        if (draft.getLocation() == null)
            throw new IllegalStateException("공연장을 선택해야 합니다.");

        List<ShowTime> showTimes = draft.getShowTimes();
        if (showTimes == null || showTimes.isEmpty())
            throw new IllegalStateException("공연 회차를 1개 이상 등록해야 합니다.");

        // 좌석 판매 방식에 따른 유효성 검사
        DomainEnums.SaleMethod saleMethod = draft.getSaleMethod();
        if (saleMethod == DomainEnums.SaleMethod.STANDING || saleMethod == DomainEnums.SaleMethod.EVENTHOST) {
            // 스탠딩 또는 주최자 배정 수량 검증
            if (draft.getSeatCount() == null || draft.getSeatCount() <= 0) {
                throw new IllegalStateException("스탠딩 또는 주최자 배정 공연의 총 수량을 입력해야 합니다.");
            }
        } else { // SCHEDULING or SELECTBYUSER
            // 좌석제 공연이라면 임시 ShowSeat(템플릿)가 존재해야 한다.
            if (!showSeatRepository.existsBySeat_Location_IdAndShowTimeIsNull(draft.getLocation().getId())) {
                throw new IllegalStateException("좌석제 공연이지만 좌석 정보가 생성되지 않았습니다.");
            }
        }

        if (draft.getTicketOptions() == null || draft.getTicketOptions().isEmpty())
            throw new IllegalStateException("티켓 옵션을 1개 이상 등록해야 합니다.");

        Message msg = messageRepository.findByShow(draft)
                .orElseThrow(() -> new IllegalStateException("메시지 정보를 입력해야 합니다."));

        if (msg.getBookingConfirmation() == null || msg.getBookingConfirmation().isBlank()
                || msg.getShowGuide() == null || msg.getShowGuide().isBlank()) {
            throw new IllegalStateException("예매 확정, 공연 안내 메시지는 필수입니다.");
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

        List<Shows> drafts = showsRepository.findByManagerAndStatus(manager, DomainEnums.ShowStatus.DRAFT);

        if (drafts.isEmpty()) {
            throw new IllegalArgumentException("삭제할 임시저장 공연이 없습니다.");
        }

        int deletedCount = drafts.size();

        for (Shows draft : drafts) {
            // 포스터 삭제
            if (draft.getPosterUrl() != null && !draft.getPosterUrl().isBlank()) {
                imageService.delete(draft.getPosterUrl());
            }

            // 상세 이미지 삭제
            if (draft.getDetailImageUrls() != null) {
                draft.getDetailImageUrls().forEach(imageService::delete);
            }
            
            // 공연 삭제
            showsRepository.delete(draft);
        }

        // API 명세에 따라 { "deletedCount": N } 반환
        return new ShowDraftDeleteResponse(deletedCount);
    }

    private void updatePoster(Shows draft, String newPosterUrl) {

        String old = draft.getPosterUrl();

        // 새 포스터가 없으면 아무것도 안 함
        if (newPosterUrl == null)
            return;

        // 기존 파일이 있고, URL이 다르면 삭제
        if (old != null && !old.equals(newPosterUrl)) {
            imageService.delete(old);
        }

        draft.setPosterUrl(newPosterUrl);
    }

    public List<String> updateDetailImages(Shows draft, ShowUpdateRequest request) {

        // DB에 저장된 이미지 목록
        List<String> existingImages = draft.getDetailImageUrls();

        // 프론트에서 유지하고 싶은 최종 이미지 목록
        List<String> finalImages =
                request.getDetailImages() == null ? List.of() : request.getDetailImages();

        // 삭제 대상 = 기존 - 유지
        List<String> deleteTargets = existingImages.stream()
                .filter(url -> !finalImages.contains(url))
                .toList();

        // S3에서 삭제
        deleteTargets.forEach(imageService::delete);

        // 엔티티에 최종 목록 반영
        draft.setDetailImageUrls(finalImages);

        // 저장
        showsRepository.save(draft);

        return finalImages;
    }

}
