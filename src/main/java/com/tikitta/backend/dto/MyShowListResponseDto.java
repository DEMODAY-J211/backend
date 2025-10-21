package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MyShowListResponseDto {
    private boolean drafted;
    private List<MyShowItemDto> published;
}
