package com.tikitta.backend.repository;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager,Long> {
    Optional<Manager> findByKakaoOauth(KakaoOauth kakaoOauth);
}
