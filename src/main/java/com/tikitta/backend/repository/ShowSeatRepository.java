package com.tikitta.backend.repository;

import com.tikitta.backend.domain.ShowSeat;
import com.tikitta.backend.domain.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowSeatRepository extends JpaRepository<ShowSeat,Long> {
    int countByShowTimeAndIsAvailable(ShowTime showTime, boolean isAvailable);

    @Modifying
    @Query("UPDATE ShowSeat ss SET ss.isAvailable = :isAvailable WHERE ss.id = :id")
    void updateIsAvailable(@Param("id") Long id, @Param("isAvailable") boolean isAvailable);
}
