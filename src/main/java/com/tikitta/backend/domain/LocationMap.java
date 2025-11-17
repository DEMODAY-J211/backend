package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "location_map")
public class LocationMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_map_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "layout_width", nullable = false)
    private Integer layoutWidth;

    @Column(name = "layout_height", nullable = false)
    private Integer layoutHeight;

    @Lob
    @Column(name = "seat_map_data", columnDefinition = "TEXT", nullable = false)
    private String seatMapData; // JSON string

    @Lob
    @Column(name = "stage_coordinates", columnDefinition = "TEXT")
    private String stageCoordinates; // JSON string of [[row, col], [row, col]]

    @Builder
    public LocationMap(Location location, Integer layoutWidth, Integer layoutHeight, String seatMapData, String stageCoordinates) {
        this.location = location;
        this.layoutWidth = layoutWidth;
        this.layoutHeight = layoutHeight;
        this.seatMapData = seatMapData;
        this.stageCoordinates = stageCoordinates;
    }

    public void setSeatMapData(String seatMapData) {
        this.seatMapData = seatMapData;
    }
}