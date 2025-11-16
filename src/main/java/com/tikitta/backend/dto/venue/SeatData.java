package com.tikitta.backend.dto.venue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatData {
    private Integer seatFloor;
    private String seatSection;
    private String seatTable;

    @JsonProperty("seat_Row")
    private Integer seatRow;

    @JsonProperty("seat_Column")
    private Integer seatColumn;
}