package com.tikitta.backend.dto;

import com.tikitta.backend.domain.DomainEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LocationLikeResponse {
    private Long id;
    private String name;
    private DomainEnums.LocationType type;
}
