package com.tikitta.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
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
    @Column(name = "oauth_role", nullable = false)
    private DomainEnums.Role role;

    @Column(name = "oauth_created", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_visited", nullable = false)
    private DomainEnums.VisitedPath visitedPath;

    @Column(name = "oauth_email")
    private String email;

    @Builder
    public KakaoOauth(Long kakaoId, String name, DomainEnums.Role role, LocalDateTime createdAt,
                      DomainEnums.VisitedPath visitedPath, String email) {
        this.kakaoId = kakaoId;
        this.name = name;
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