package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResponse {
    private List<List<Object>> seat;
    private List<ReservationSeatListResponse> reservation;
}
