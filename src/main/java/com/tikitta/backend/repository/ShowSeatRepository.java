package com.tikitta.backend.repository;

import com.tikitta.backend.domain.ShowSeat;
import com.tikitta.backend.domain.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowSeatRepository extends JpaRepository<ShowSeat,Long> {
    int countByShowTimeAndIsAvailable(ShowTime showTime, boolean isAvailable);}
