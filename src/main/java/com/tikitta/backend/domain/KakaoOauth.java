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
@Table(name = "kakao_oauth")
public class KakaoOauth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_id")
    private Long id;

    private Long kakaoId;

    @Column(name = "oauth_name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_gender")
    private DomainEnums.Gender gender;

    @Column(name = "oauth_age")
    private Integer age;

    @Column(name = "oauth_phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_role", nullable = false)
    private DomainEnums.Role role;

    @Column(name = "oauth_created", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_visited", nullable = false)
    private DomainEnums.VisitedPath visitedPath;

    @Column(name = "oauth_email")
    private String email;

    @Builder
    public KakaoOauth(Long kakaoId, String name, DomainEnums.Gender gender, Integer age,
                      String phone, DomainEnums.Role role, LocalDateTime createdAt,
                      DomainEnums.VisitedPath visitedPath, String email) {
        this.kakaoId = kakaoId;
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.phone = phone;
        this.role = role != null ? role : DomainEnums.Role.USER; // 기본값 USER
        this.createdAt = createdAt;
        this.visitedPath = visitedPath;
        this.email = email;
    }

    public KakaoOauth update(String name) {
        this.name = name;
        return this;
    }
}