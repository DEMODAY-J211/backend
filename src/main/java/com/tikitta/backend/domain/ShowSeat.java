package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "show_seat")
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_seat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_time_id", nullable = false)
    private ShowTime showTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    @Column(name = "is_good_seat")
    private Boolean isGoodSeat;

    @Builder
    public ShowSeat(ShowTime showTime, Seat seat, boolean isAvailable, Boolean isGoodSeat) {
        this.showTime = showTime;
        this.seat = seat;
        this.isAvailable = isAvailable;
        this.isGoodSeat = isGoodSeat;
    }

    // [추가] 예매 시 좌석을 점유하는 메소드
    public void reserve() {
        if (!this.isAvailable) {
            throw new IllegalStateException("이미 예약된 좌석입니다.");
        }
        this.isAvailable = false;
    }

    // [추가] 예매 취소 시 좌석을 반환하는 메소드
    public void cancel() {
        this.isAvailable = true;
    }
}