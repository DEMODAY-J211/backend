package com.tikitta.backend.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationStatusUpdateRequest {
    private List<ReservationStatusInfo> reservations;
}
