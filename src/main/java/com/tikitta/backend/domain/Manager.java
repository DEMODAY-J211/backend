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
@Table(name = "manager")
public class Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manager_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oauth_id", unique = true)
    private KakaoOauth kakaoOauth;

    @Column(name = "manager_name", nullable = false)
    private String name;

    @Column(name = "manager_picture")
    private String pictureUrl;

    @Column(name = "manager_intro", nullable = false)
    private String introduction;

    @Column(name = "manager_text", columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "manager_urls", joinColumns = @JoinColumn(name = "manager_id"))
    @Column(name = "url")
    private List<String> urls = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "manager_location_likes",
            joinColumns = @JoinColumn(name = "manager_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    private List<Location> likedLocations = new ArrayList<>();

    @Builder
    public Manager(KakaoOauth kakaoOauth, String name, String pictureUrl, String introduction, String description) {
        this.kakaoOauth = kakaoOauth;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.introduction = introduction;
        this.description = description;
    }
}