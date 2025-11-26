package com.tikitta.backend.repository;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.Reservation;
import com.tikitta.backend.domain.ReservationItem;
import com.tikitta.backend.domain.ShowSeat;
import com.tikitta.backend.domain.ShowTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReservationItemRepository extends JpaRepository<ReservationItem,Long> {
    // 👇 [추가] 특정 회차(showTime)의 ReservationItem 중 가장 큰 entryNumber를 조회
    @Query("SELECT MAX(ri.entryNumber) FROM ReservationItem ri WHERE ri.reservation.showTime = :showTime")
    Integer findMaxEntryNumberByShowTime(@Param("showTime") ShowTime showTime);


    List<ReservationItem> findByReservation(Reservation reservation);

    @Transactional
    @Modifying
    @Query("UPDATE ReservationItem ri SET ri.status = :status WHERE ri.reservation.id = :reservationId")
    void updateStatusByReservationId(@Param("reservationId") Long reservationId, @Param("status") DomainEnums.ReservationStatus status);


    //좌석별 조회
    @Query("SELECT ri FROM ReservationItem ri " +
            "JOIN FETCH ri.reservation r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH ri.showSeat ss " +
            "JOIN FETCH ss.seat seat " +
            "WHERE ss.showTime = :showTime " +
            "ORDER BY r.createdAt DESC")
    List<ReservationItem> findReservationItemsByShowTime(@Param("showTime") ShowTime showTime);

    @Query("SELECT ri FROM ReservationItem ri " +
            "JOIN FETCH ri.reservation r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH ri.showSeat ss " +
            "JOIN FETCH ss.seat seat " +
            "WHERE ss.showTime = :showTime " +
            "AND (u.name LIKE %:keyword% OR seat.seatNumber LIKE %:keyword%) " +
            "ORDER BY r.createdAt DESC")
    List<ReservationItem> findReservationItemsByShowTimeAndKeyword(
            @Param("showTime") ShowTime showTime,
            @Param("keyword") String keyword);

    Optional<ReservationItem> findByShowSeat(ShowSeat showSeat);
}
