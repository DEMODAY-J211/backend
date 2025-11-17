package com.tikitta.backend.dto.userbooking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponseDto {

    private Long showSeatId;   // ShowSeat.id → 예매 선택할때 사용할 PK
    private Long seatId;       // Seat.id → 필요하면 사용

    private Integer seatFloor;
    private String seatSection;
    private Integer seatRow;
    private Integer seatColumn;

    private String seatTable;  // "A3-7" 형태의 표기 (편의용)

    private Boolean isAvailable;
}