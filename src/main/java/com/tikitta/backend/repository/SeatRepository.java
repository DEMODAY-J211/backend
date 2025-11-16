package com.tikitta.backend.repository;

import com.tikitta.backend.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat,Long> {
    @Modifying
    @Transactional
    void deleteByLocationIdAndSeatNumberIn(Long locationId, List<String> seatNumbers);

    List<Seat> findByLocationIdAndSeatNumberIn(Long locationId, List<String> seatNumbers);
}