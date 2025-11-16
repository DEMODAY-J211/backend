package com.tikitta.backend.dto.venue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VenueSeatmapResponse {
    private String location;

    @JsonProperty("layout_width")
    private Integer layoutWidth;

    @JsonProperty("layout_height")
    private Integer layoutHeight;

    @JsonProperty("seat_map")
    private List<List<Object>> seatMap;
}