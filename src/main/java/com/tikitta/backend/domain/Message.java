package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false, unique = true)
    private Shows show;

    @Column(name = "message_pay_guide")
    private String paymentGuide; //입금안내

    @Column(name = "message_book_confirm")
    private String bookingConfirmation; //예매확정

    @Column(name = "message_book_custom", nullable = false)
    private String bookingCustom; //예매확정-필수문장

    @Column(name = "message_show_guide")
    private String showGuide; //공연안내

    @Column(name = "message_show_qr", nullable = false)
    private String qrGuide; //공연안내-큐알

    @Column(name = "message_review_request")
    private String reviewRequest; //공연후기

    @Builder
    public Message(Shows show, String paymentGuide, String bookingConfirmation, String bookingCustom, String showGuide, String qrGuide, String reviewRequest) {
        this.show = show;
        this.paymentGuide = paymentGuide;
        this.bookingConfirmation = bookingConfirmation;
        this.bookingCustom = bookingCustom;
        this.showGuide = showGuide;
        this.qrGuide = qrGuide;
        this.reviewRequest = reviewRequest;
    }
}