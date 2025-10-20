package com.tikitta.backend.dto;

import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.domain.Shows;
import com.tikitta.backend.domain.TicketOption;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ShowDetailResponse {
    private Long showId;
    private String showTitle;
    private LocalDateTime showStartDate;
    private LocalDateTime showtimeEndDate;
    private String showLocation;
    private String showPosterPicture;
    private List<ShowTimeDto> showtimeList;
    private List<TicketOptionDto> ticketOptionList; // ◀ JSON 예시와 달리 List로 구현
    private ManagerInfoDto managerInfo;
    private String showDetailText;

    public ShowDetailResponse(Shows show){
        this.showId = show.getId();
        this.showTitle = show.getTitle();
        this.showLocation = show.getLocation().getName();
        this.showPosterPicture = show.getPosterUrl();
        this.showDetailText = show.getDetailText();

        ShowTime earliest = show.getShowTimes().stream().min(Comparator.comparing(ShowTime::getStartAt)).get();
        ShowTime latest = show.getShowTimes().stream().max(Comparator.comparing(ShowTime::getStartAt)).get();

        this.showStartDate = earliest.getStartAt();
        this.showtimeEndDate = latest.getStartAt();

        this.showtimeList = show.getShowTimes().stream()
                .map(ShowTimeDto::new)
                .collect(Collectors.toList());

        this.ticketOptionList = show.getTicketOptions().stream()
                .map(TicketOptionDto::new)
                .collect(Collectors.toList());

        this.managerInfo = new ManagerInfoDto(show.getManager());
    }

    @Getter
    private static class ShowTimeDto {
        private Long showtimeId;
        private LocalDateTime showtimeStart;

        public ShowTimeDto(ShowTime showTime) {
            this.showtimeId = showTime.getId();
            this.showtimeStart = showTime.getStartAt();
        }
    }

    @Getter
    private static class TicketOptionDto {
        private String ticketoptionName;
        private Integer ticketoptionPrice;

        public TicketOptionDto(TicketOption ticketOption) {
            this.ticketoptionName = ticketOption.getName();
            this.ticketoptionPrice = ticketOption.getPrice();
        }
    }

    @Getter
    private static class ManagerInfoDto {
        private String managerName;
        private String managerEmail; // ◀ JSON 예시와 다르지만, 기존 코드를 따름

        public ManagerInfoDto(Manager manager) {
            this.managerName = manager.getName();
            this.managerEmail = manager.getKakaoOauth().getEmail();
        }
    }
}
