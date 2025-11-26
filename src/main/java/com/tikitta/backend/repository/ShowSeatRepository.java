package com.tikitta.backend.repository;

import com.tikitta.backend.domain.ShowSeat;
import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.domain.Shows;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShowSeatRepository extends JpaRepository<ShowSeat,Long> {
    int countByShowTimeAndIsAvailable(ShowTime showTime, boolean isAvailable);

    @Modifying
    @Query("UPDATE ShowSeat ss SET ss.isAvailable = :isAvailable WHERE ss.id = :id")
    void updateIsAvailable(@Param("id") Long id, @Param("isAvailable") boolean isAvailable);

    @Modifying
    @Query("DELETE FROM ShowSeat ss WHERE ss.showTime IN (SELECT st FROM ShowTime st WHERE st.show = :show)")
    void deleteByShow(@Param("show") Shows show);

    boolean existsByShowTime_Show(Shows showTimeShow);

    List<ShowSeat> findByShowTime(ShowTime showTime);

    @Query("SELECT ss FROM ShowSeat ss JOIN ss.seat s WHERE s.location.id = :locationId AND ss.showTime IS NULL")
    List<ShowSeat> findDraftShowSeatsByLocationId(@Param("locationId") Long locationId);

    boolean existsBySeat_Location_IdAndShowTimeIsNull(Long locationId);

    List<ShowSeat> findBySeat_Location_IdAndShowTimeIsNull(Long locationId);

    @Modifying
    void deleteBySeat_Location_IdAndShowTimeIsNull(Long locationId);

    Optional<ShowSeat> findByShowTimeIdAndSeat_SeatNumber(Long showTimeId, String seatNumber);
}
