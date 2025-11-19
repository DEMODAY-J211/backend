package com.tikitta.backend.dto.userbooking;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChangeSeatsRequest {
    private List<Long> showSeatIds;
}