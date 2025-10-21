package com.tikitta.backend.dto;

import com.tikitta.backend.domain.Shows;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MyShowItemDto {
    private Long id;
    private String title;
    private String poster;

    public static MyShowItemDto fromEntity(Shows show) {
        return new MyShowItemDto(
                show.getId(),
                show.getTitle(),
                show.getPosterUrl()
        );
    }
}
