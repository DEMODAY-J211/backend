package com.tikitta.backend.dto.venue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationLikeResponse {
    private Long managerId;
    private Long locationId;
}
