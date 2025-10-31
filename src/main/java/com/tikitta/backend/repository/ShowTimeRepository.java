package com.tikitta.backend.repository;

import com.tikitta.backend.domain.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowTimeRepository extends JpaRepository<ShowTime, Long> {
    List<ShowTime> findAllByStartAtBetween(LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query("UPDATE ShowTime st SET st.remainSeatCount = st.remainSeatCount + :quantity WHERE st.id = :id")
    void increaseRemainSeat(@Param("id") Long id, @Param("quantity") Integer quantity);
}
