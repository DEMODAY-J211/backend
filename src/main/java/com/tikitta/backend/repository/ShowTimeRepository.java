package com.tikitta.backend.repository;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowTimeRepository extends JpaRepository<ShowTime, Long> {
    List<ShowTime> findAllByStartAtBetween(LocalDateTime from, LocalDateTime to);
    List<ShowTime> findAllByEndAtBetween(LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query("UPDATE ShowTime st SET st.remainSeatCount = st.remainSeatCount + :quantity WHERE st.id = :id")
    void increaseRemainSeat(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Query("SELECT st FROM ShowTime st JOIN st.show s WHERE s.saleMethod = :saleMethod AND st.startAt BETWEEN :from AND :to")
    List<ShowTime> findShowsToAssignSeats(
            @Param("saleMethod") DomainEnums.SaleMethod saleMethod,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
