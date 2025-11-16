package com.tikitta.backend.dto.venue;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatDeleteResponse {
    private long updatedSeatCount;
    private long totalAvailableSeats;
}