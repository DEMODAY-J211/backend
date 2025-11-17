package com.tikitta.backend.dto.venue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SeatVipRequest {
    @JsonProperty("seat_map")
    private List<List<Object>> seatMap;
}