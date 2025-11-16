package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seat")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "seat_floor", nullable = false)
    private Integer floor = 1;

    @Column(name = "seat_section", nullable = false)
    private String section;

    @Column(name = "seat_row", nullable = false)
    private Integer seatRow;

    @Column(name = "seat_column", nullable = false)
    private Integer seatColumn;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Builder
    public Seat(Location location, Integer floor, String section, Integer seatRow, Integer seatColumn, String seatNumber) {
        this.location = location;
        this.floor = floor;
        this.section = section;
        this.seatRow = seatRow;
        this.seatColumn = seatColumn;
        this.seatNumber = seatNumber;
    }
}