package com.tikitta.backend.dto.venue;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatVipResponse {
    private long updatedSeatCount;
    private long totalAvailableSeats;
}