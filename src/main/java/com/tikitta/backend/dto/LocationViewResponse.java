package com.tikitta.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LocationViewResponse {
    private final Long locationId;
    private final String locationName;
    private final String locationAddress;
    private final String locationSeatPicture;
    private final Integer locationSeatTotalCount;
    private final Integer locationFloor;
    private final boolean locationLike;

    @Builder
    public LocationViewResponse(Long locationId, String locationName, String locationAddress, String locationSeatPicture, Integer locationSeatTotalCount, Integer locationFloor, boolean locationLike) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.locationAddress = locationAddress;
        this.locationSeatPicture = locationSeatPicture;
        this.locationSeatTotalCount = locationSeatTotalCount;
        this.locationFloor = locationFloor;
        this.locationLike = locationLike;
    }
}
