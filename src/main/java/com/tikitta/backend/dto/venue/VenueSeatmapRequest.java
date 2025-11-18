package com.tikitta.backend.dto.venue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class VenueSeatmapRequest {
    @JsonProperty("location_id")
    private Long locationId;
    @JsonProperty("layout_width")
    private int layoutWidth;
    @JsonProperty("layout_height")
    private int layoutHeight;
    @JsonProperty("seat_map")
    private List<List<Object>> seatMap;
    @JsonProperty("seat_data")
    private Map<String, SeatData> seatData;
}