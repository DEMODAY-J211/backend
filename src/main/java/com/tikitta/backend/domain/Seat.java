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
    private String seatRow; // 행(row)은 'A', 'B'와 같을 수 있으므로 String이 적합

    @Column(name = "seat_column", nullable = false)
    private String seatCol; // 열(column) 또한 숫자가 아닐 수 있음

    @Column(name = "seat_number", nullable = false)
    private String seatNumber; // 예: 'A열 5번'

    @Builder
    public Seat(Location location, Integer floor, String section, String seatRow, String seatCol, String seatNumber) {
        this.location = location;
        this.floor = floor;
        this.section = section;
        this.seatRow = seatRow;
        this.seatCol = seatCol;
        this.seatNumber = seatNumber;
    }
}