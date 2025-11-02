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
@Table(name = "reservation_item")
public class ReservationItem {//개별티켓

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    // 좌석제 공연일 경우에만 값이 존재
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_seat_id")
    private ShowSeat showSeat;

    // 스탠딩 공연일 경우에만 값이 존재
    @Column(name = "reservation_entry_number")
    private Integer entryNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false)
    private DomainEnums.ReservationStatus status;

    @Column(name = "reservation_item_qr")
    private String qrCodeUrl;

    @Column(name = "reservation_item_enter_time")
    private LocalDateTime enteredAt;

    @Column(name = "is_entered", nullable = false)
    private boolean isEntered = false;

    @Builder
    public ReservationItem(Reservation reservation, ShowSeat showSeat, Integer entryNumber, String qrCodeUrl, DomainEnums.ReservationStatus status) {
        this.reservation = reservation;
        this.showSeat = showSeat;
        this.entryNumber = entryNumber;
        this.qrCodeUrl = qrCodeUrl;
        this.status = status;
    }

    // 입장 처리 로직
    public void checkIn() {
        if (!this.isEntered) {
            this.isEntered = true;
            this.enteredAt = LocalDateTime.now();
        }
    }

    // QR 코드 URL 설정 메서드
    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
}