package com.tikitta.backend.dto;

import com.tikitta.backend.domain.DomainEnums;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShowUpdateRequest {
    private String title;
    private String poster;
    private List<ShowTimeInfo> showTimes;
    private LocalDateTime bookStart;
    private List<TicketOptionInfo> ticketOptions;
    private String bankMaster;
    private String bankName;
    private String bankAccount;
    private List<String> detailImages;
    private String detailText;
    private Long locationId;
    private String locationName;
    private String seatType;
    private Long seatCount;
    private ShowMessageInfo showMessage;
    private String status;
    private String reviewUrl;

    @Getter
    @NoArgsConstructor
    public static class ShowTimeInfo{
        private LocalDateTime showStart;
        private LocalDateTime showEnd;
    }

    @Getter
    @NoArgsConstructor
    public static class TicketOptionInfo{
        private String name;
        private String description;
        private Long amount;
        private Integer price;
    }

    @Getter
    @NoArgsConstructor
    public static class ShowMessageInfo{
        private String payGuide;
        private String bookConfirm;
        private String showGuide;
        private String reviewRequest;
        private String reviewUrl;
    }

}
