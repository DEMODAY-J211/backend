package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "manager")
@Builder
public class Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manager_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oauth_id", unique = true)
    private KakaoOauth kakaoOauth;

    @Column(name = "manager_name", nullable = false)
    @Setter
    private String name;

    @Column(name = "manager_picture")
    @Setter
    private String pictureUrl;

    @Column(name = "manager_intro", nullable = false)
    @Setter
    private String introduction;

    @Column(name = "manager_text", columnDefinition = "TEXT")
    @Setter
    private String description;

    @ElementCollection
    @Setter
    @CollectionTable(name = "manager_urls", joinColumns = @JoinColumn(name = "manager_id"))
    @Column(name = "url")
    @Builder.Default
    private List<String> urls = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "manager_location_likes",
            joinColumns = @JoinColumn(name = "manager_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )

    @Builder.Default
    private List<Location> likedLocations = new ArrayList<>();


}