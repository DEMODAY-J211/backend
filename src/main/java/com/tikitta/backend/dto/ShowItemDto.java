package com.tikitta.backend.dto;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.Location;
import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.domain.Shows;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;

@Getter
public class ShowItemDto {
    private Long showId;
    private String showTitle;
    private LocalDateTime showTimes;
    private String showLocation;
    private String showPosterPicture;
    private boolean isReservable;
    private boolean reserved;

    public ShowItemDto(Shows show){
        this(show, false);
    }

    public ShowItemDto(Shows show, boolean reserved) {
        this.showId = show.getId();
        this.showTitle = show.getTitle();
        this.showPosterPicture = show.getPosterUrl();
        
        Location location = show.getLocation();
        this.showLocation = (location != null) ? location.getName() : null;

        this.isReservable = show.getStatus() == DomainEnums.ShowStatus.PUBLISHED &&
                show.getBookingStartAt() != null &&
                show.getBookingStartAt().isBefore(LocalDateTime.now());

        // 가장 빠른 공연 시간 설정
        show.getShowTimes().stream()
                .min(Comparator.comparing(ShowTime::getStartAt))
                .ifPresent(earliest -> this.showTimes = earliest.getStartAt());

        // ✅ 로그인한 사용자의 예매 여부 반영
        this.reserved = reserved;

    }
}
