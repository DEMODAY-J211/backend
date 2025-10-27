package com.tikitta.backend.repository;

import com.tikitta.backend.domain.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowTimeRepository extends JpaRepository<ShowTime, Long> {
    List<ShowTime> findAllByStartAtBetween(LocalDateTime from, LocalDateTime to);
}
