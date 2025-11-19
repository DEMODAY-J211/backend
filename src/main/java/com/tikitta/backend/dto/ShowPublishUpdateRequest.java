package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class ShowPublishUpdateRequest {
    private String poster;
    private List<String> detailImages;
    private String detailText;
    private String status;
}
