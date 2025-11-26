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

    private Long showtimeId;

    @Builder.Default
    private List<CheckinStatusUpdateItem> checkinStatusUpdateRequest = new ArrayList<>();

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckinStatusUpdateItem {
        private String seat;
        private Boolean isReserved;
        private Boolean isEntered;
    }
}
