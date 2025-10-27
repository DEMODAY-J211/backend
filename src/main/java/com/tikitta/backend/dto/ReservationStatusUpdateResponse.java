package com.tikitta.backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusUpdateResponse {
    private int updatedCount;
    private List<Long> failedIds;
}
