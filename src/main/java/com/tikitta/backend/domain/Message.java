package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
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
    private String paymentGuide;

    @Column(name = "message_book_confirm")
    private String bookingConfirmation;

    @Column(name = "message_book_custom", nullable = false)
    private String bookingCustom;

    @Column(name = "message_show_guide")
    private String showGuide;

    @Column(name = "message_show_qr", nullable = false)
    private String qrGuide;

    @Column(name = "message_review_request")
    private String reviewRequest;

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