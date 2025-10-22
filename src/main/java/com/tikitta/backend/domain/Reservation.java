package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @Column(name = "reservation_number", nullable = false, unique = true)
    private String reservationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private KakaoOauth user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_time_id", nullable = false)
    private ShowTime showTime;

    @Column(name = "reservation_quantity", nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "reservation_refund_account", nullable = false)
    private String refundAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false)
    private DomainEnums.ReservationStatus status;

    @Column(name = "reservation_date", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationItem> reservationItems = new ArrayList<>();

    @Builder
    public Reservation(String reservationNumber, KakaoOauth user, ShowTime showTime, Integer quantity, Integer totalPrice, String refundAccountNumber, DomainEnums.ReservationStatus status, LocalDateTime createdAt) {
        this.reservationNumber = reservationNumber;
        this.user = user;
        this.showTime = showTime;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.refundAccountNumber = refundAccountNumber;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}