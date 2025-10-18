package com.tikitta.backend.repository;

import com.tikitta.backend.domain.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowTimeRepository extends JpaRepository<ShowTime,Long> {
}
