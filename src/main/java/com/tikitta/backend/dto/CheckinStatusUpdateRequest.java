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
        private Long reservationItemId;
        private Boolean isEntered;
        private Boolean isReserved;
    }
}
