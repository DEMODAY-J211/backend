package com.tikitta.backend.dto.venue;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueRegisterRequest {
    private String locationName;
    private String locationAddress;
    private String locationAddressDetail;
    private Integer locationStandingCount;
    private Integer locationSeatfloor;
    private Integer locationSeatCount;
}