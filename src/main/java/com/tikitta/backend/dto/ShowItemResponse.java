package com.tikitta.backend.dto;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.domain.Shows;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Getter
public class ShowItemResponse {
    private Long showId;
    private String showTitle;
    private LocalDateTime showTimes;
    private String showLocation;
    private String showPosterPicture;
    private boolean isReservable;

    public ShowItemResponse(Shows show){
        this.showId = show.getId();
        this.showTitle = show.getTitle();
        this.showPosterPicture = show.getPosterUrl();
        this.showLocation = show.getLocation().getName();
        this.isReservable = show.getStatus() == DomainEnums.ShowStatus.PUBLISHED &&
                show.getBookingStartAt() != null &&
                show.getBookingStartAt().isBefore(LocalDateTime.now());

        // 1. startAt 기준으로 리스트를 정렬하여 가장 빠른 ShowTime을 찾음
        ShowTime earliestShowTime = show.getShowTimes().stream()
                .min(Comparator.comparing(ShowTime::getStartAt))
                .get(); // ◀ .isEmpty() 체크를 통과했으므로 .get()은 안전

        // 2. "YYYY-MM-DD" 형식으로 포맷팅
        this.showTimes = earliestShowTime.getStartAt();

    }
}
