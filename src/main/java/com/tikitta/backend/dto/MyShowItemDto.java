package com.tikitta.backend.dto;

import com.tikitta.backend.domain.Shows;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MyShowItemDto {
    private Long showId; // 필드 이름을 showId로 변경
    private String title;
    private String poster;

    public static MyShowItemDto fromEntity(Shows show) {
        return new MyShowItemDto(
                show.getId(), // Shows 엔티티의 ID를 showId에 매핑
                show.getTitle(),
                show.getPosterUrl()
        );
    }
}
