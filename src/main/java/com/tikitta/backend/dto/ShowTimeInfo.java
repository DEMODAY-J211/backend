package com.tikitta.backend.dto;

import com.tikitta.backend.domain.ShowTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ShowTimeInfo {
    private LocalDateTime showTime;
    private Long showTimeId;

    public static ShowTimeInfo fromEntity(ShowTime showTime) {
        return new ShowTimeInfo(
                showTime.getStartAt(),
                showTime.getId()
        );
    }
}
