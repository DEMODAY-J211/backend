package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "location")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long id;

    @Column(name = "location_name", nullable = false)
    private String name;

    @Column(name = "location_address", nullable = false)
    private String address;

    @Column(name = "location_address_detail")
    private String addressDetail;

    @Column(name = "location_seat_picture")
    private String seatPictureUrl;

    // 참고: 엑셀 파일은 보통 S3 같은 파일 스토리지에 저장하고 URL만 DB에 저장합니다.
    @Column(name = "location_seat_excel")
    private String seatExcelUrl;

    @Column(name = "location_seat_total_count", nullable = false)
    private Integer totalSeats;

    @Column(name = "location_floor", nullable = false)
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false)
    private DomainEnums.LocationType type;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    @Builder
    public Location(String name, String address, String addressDetail, String seatPictureUrl, String seatExcelUrl, Integer totalSeats, Integer floor, DomainEnums.LocationType type) {
        this.name = name;
        this.address = address;
        this.addressDetail = addressDetail;
        this.seatPictureUrl = seatPictureUrl;
        this.seatExcelUrl = seatExcelUrl;
        this.totalSeats = totalSeats;
        this.floor = floor;
        this.type = type;
    }
}