package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "show_time")
public class ShowTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_time_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Shows show;

    @Column(name = "show_time_start", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "show_time_end", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "show_time_online_book_end", nullable = false)
    private LocalDateTime bookingEndAt;

    @Column(name = "show_time_standing_quantity")
    private Integer totalStandingQuantity;

    @Builder
    public ShowTime(Shows show, LocalDateTime startAt, LocalDateTime endAt, LocalDateTime bookingEndAt,  Integer totalStandingQuantity) {
        this.show = show;
        this.startAt = startAt;
        this.endAt = endAt;
        this.bookingEndAt = bookingEndAt;
        this.totalStandingQuantity = totalStandingQuantity;
    }
}