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
@Table(name = "shows") // 'show'는 SQL 예약어일 수 있으므로 'shows'로 변경
public class Shows {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "show_title", nullable = false)
    private String title;

    @Column(name = "show_poster_picture")
    private String posterUrl;

    @Column(name = "show_book_start")
    private LocalDateTime bookingStartAt;

    @Column(name = "show_bank_master")
    private String bankDepositorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "show_bank")
    private DomainEnums.Bank bankName;

    @Column(name = "show_bank_number")
    private String bankAccountNumber;

    @ElementCollection
    @CollectionTable(name = "show_detail_pictures", joinColumns = @JoinColumn(name = "show_id"))
    @Column(name = "picture_url")
    private List<String> detailImageUrls = new ArrayList<>();

    @Column(name = "show_detail_text", columnDefinition = "TEXT")
    private String detailText;

    @Enumerated(EnumType.STRING)
    @Column(name = "show_sale_method")
    private DomainEnums.SaleMethod saleMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "show_status", nullable = false)
    private DomainEnums.ShowStatus status;

    @Column(name = "show_review_url")
    private String reviewUrl;

    @Column(name = "show_user_link", unique = true)
    private String userLink;

    @Column(name = "show_is_completed", nullable = false)
    private boolean isCompleted = false;

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowTime> showTimes = new ArrayList<>();

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketOption> ticketOptions = new ArrayList<>();

    @Builder
    public Shows(Manager manager, Location location, String title, String posterUrl, LocalDateTime bookingStartAt, String bankDepositorName, DomainEnums.Bank bankName, String bankAccountNumber, String detailText, DomainEnums.SaleMethod saleMethod, DomainEnums.ShowStatus status, String reviewUrl, String userLink) {
        this.manager = manager;
        this.location = location;
        this.title = title;
        this.posterUrl = posterUrl;
        this.bookingStartAt = bookingStartAt;
        this.bankDepositorName = bankDepositorName;
        this.bankName = bankName;
        this.bankAccountNumber = bankAccountNumber;
        this.detailText = detailText;
        this.saleMethod = saleMethod;
        this.status = status != null ? status : DomainEnums.ShowStatus.DRAFT;
        this.reviewUrl = reviewUrl;
        this.userLink = userLink;
    }
}