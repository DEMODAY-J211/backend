package com.tikitta.backend.dto;

import com.tikitta.backend.domain.ShowTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckinShowTimeDto {
    private LocalDateTime showTime;
    private Long showTimeId;

    public static CheckinShowTimeDto fromEntity(ShowTime showTime) {
        return new CheckinShowTimeDto(showTime.getStartAt(), showTime.getId());
    }
}
