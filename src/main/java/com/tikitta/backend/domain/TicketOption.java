package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ticket_option")
public class TicketOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_option_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Shows show;

    @Column(name = "ticket_option_name", nullable = false)
    private String name;

    @Column(name = "ticket_option_detail")
    private String description;

    @Column(name = "ticket_option_count", nullable = false)
    private Integer quantity;

    @Column(name = "ticket_option_price", nullable = false)
    private Integer price;

    @Builder
    public TicketOption(Shows show, String name, String description, Integer quantity, Integer price) {
        this.show = show;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.price = price;
    }
}