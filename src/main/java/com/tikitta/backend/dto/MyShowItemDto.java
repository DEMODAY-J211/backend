package com.tikitta.backend.dto;

import com.tikitta.backend.domain.Shows;
import com.tikitta.backend.domain.ShowTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MyShowItemDto {
    private Long showId;
    private String title;
    private String poster;
    private List<ShowTimeDto> showTimeList;

    public static MyShowItemDto fromEntity(Shows show) {
        List<ShowTimeDto> showTimeDtoList = show.getShowTimes().stream()
                .map(ShowTimeDto::fromEntity)
                .collect(Collectors.toList());

        return new MyShowItemDto(
                show.getId(),
                show.getTitle(),
                show.getPosterUrl(),
                showTimeDtoList
        );
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShowTimeDto {
        private Long showTimeId;
        private LocalDateTime showTime;

        public static ShowTimeDto fromEntity(ShowTime showTime) {
            return new ShowTimeDto(showTime.getId(), showTime.getStartAt());
        }
    }
}
