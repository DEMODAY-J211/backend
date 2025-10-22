package com.tikitta.backend.repository;

import com.tikitta.backend.domain.ReservationItem;
import com.tikitta.backend.domain.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationItemRepository extends JpaRepository<ReservationItem,Long> {
    // 👇 [추가] 특정 회차(showTime)의 ReservationItem 중 가장 큰 entryNumber를 조회
    @Query("SELECT MAX(ri.entryNumber) FROM ReservationItem ri WHERE ri.reservation.showTime = :showTime")
    Integer findMaxEntryNumberByShowTime(@Param("showTime") ShowTime showTime);
}
