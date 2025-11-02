package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckinStatusUpdateRequest {

    private List<CheckinStatusUpdateItem> checkinStatusUpdateRequest;

    //일괄 처리를 위해 List로 담음. List에 넣을 객체 생성
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckinStatusUpdateItem {
        private Long reservationItemId; //있을 때만 사용
        private Boolean isEntered;
        private Boolean isReserved;

        //좌석제일 경우
        private Long showSeatId;

        //스탠딩 공연일 경우
        //TODO 이거 번호 기존 예약건과 겹치지 않게 어떤식으로 업데이트 할 건지 생각해봐야할 듯
        private Integer entryNumber;
    }
}
