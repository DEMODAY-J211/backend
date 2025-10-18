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

    // url을 리스트로 한번에 빌드하기 위해 빌더를 클래스 밖으로 뺐음

//    @Builder
//    public Manager(KakaoOauth kakaoOauth, String name, String pictureUrl, String introduction, String description) {
//        this.kakaoOauth = kakaoOauth;
//        this.name = name;
//        this.pictureUrl = pictureUrl;
//        this.introduction = introduction;
//        this.description = description;
//        this.urls = new ArrayList<>();
//    }
}