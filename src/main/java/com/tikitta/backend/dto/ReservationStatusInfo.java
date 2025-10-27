package com.tikitta.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationStatusInfo {
    private Long reservationId;
    private String name;
    private String status;
    private boolean isReserved;
}
