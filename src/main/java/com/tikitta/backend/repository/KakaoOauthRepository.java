package com.tikitta.backend.repository;

import com.tikitta.backend.domain.KakaoOauth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KakaoOauthRepository extends JpaRepository<KakaoOauth,Long> {
    Optional<KakaoOauth> findByEmail(String email);
}
