package com.tikitta.backend.dto;

import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.domain.Shows;
import com.tikitta.backend.domain.TicketOption;
import com.tikitta.backend.repository.ShowSeatRepository;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class BookingInfoResponse {
    private Long showId;
    private String showTitle;
    private List<ShowTimeItemDto> showtimeList;
    private List<TicketOptionItemDto> ticketOptionList;

    public BookingInfoResponse(Shows show, List<ShowTimeItemDto> showTimeList){
        this.showId = show.getId();
        this.showTitle = show.getTitle();
        this.showtimeList = showTimeList;

        this.ticketOptionList = show.getTicketOptions().stream()
                .map(TicketOptionItemDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class ShowTimeItemDto{
        private Long showtimeId;
        private LocalDateTime showtimeStart;
        private int availableSeats;

        public ShowTimeItemDto(ShowTime showtime, int availableSeats){
            this.showtimeId = showtime.getId();
            this.showtimeStart = showtime.getStartAt();
            this.availableSeats = availableSeats;
        }
    }

    @Getter
    private static class TicketOptionItemDto {
        private Long ticketOptionId;
        private String ticketOptionName;
        private Integer ticketOptionPrice;

        public TicketOptionItemDto(TicketOption ticketOption) {
            this.ticketOptionId = ticketOption.getId();
            this.ticketOptionName = ticketOption.getName();
            this.ticketOptionPrice = ticketOption.getPrice();
        }
    }
}
