package com.tikitta.backend.dto;

import com.tikitta.backend.domain.TicketOption;
import lombok.Getter;

@Getter
public class TicketOptionDto {
    private String ticketoptionName;
    private Integer ticketoptionPrice;

    public TicketOptionDto(TicketOption ticketOption){
        this.ticketoptionName = ticketOption.getName();
        this.ticketoptionPrice = ticketOption.getPrice();
    }
}
