package com.tikitta.backend.dto;

import com.tikitta.backend.domain.TicketOption;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailDto {
    private Long ticketOptionId;
    private String ticketOptionName;
    private Integer ticketPrice;
    private Integer quantity;

    public TicketDetailDto(TicketOption ticketOption, Integer quantity) {
        this.ticketOptionId = ticketOption.getId();
        this.ticketOptionName = ticketOption.getName();
        this.ticketPrice = ticketOption.getPrice();
        this.quantity = quantity;
    }
}
