package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinStatusUpdateRequest {

    @Builder.Default
    private List<CheckinStatusUpdateItem> checkinStatusUpdateRequest = new ArrayList<>();

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckinStatusUpdateItem {
        private Long reservationItemId;
        private Boolean isReserved;
        private Boolean isEntered;
        private Long showSeatId;
        private Integer entryNumber;
    }
}
