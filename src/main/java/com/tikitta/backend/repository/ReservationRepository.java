package com.tikitta.backend.repository;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.Reservation;
import com.tikitta.backend.domain.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {
    List<Reservation> findByShowTimeAndStatusIn(ShowTime showTime, List<DomainEnums.ReservationStatus> statuses);

    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.showTime st " +
            "JOIN FETCH st.show s " +
            "JOIN FETCH s.location l " +
            "LEFT JOIN FETCH r.reservationItems ri " +
            "LEFT JOIN FETCH ri.showSeat rss " +
            "LEFT JOIN FETCH rss.seat seat " +
            "WHERE r.id = :reservationId")
    Optional<Reservation> findByIdWithDetails(@Param("reservationId") Long reservationId);

    // ▼▼▼ 수정된 메소드 ▼▼▼
    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.ticketOption " + // 이제 이 JOIN이 정상적으로 동작합니다.
            "WHERE r.showTime = :showTime " +
            "ORDER BY r.createdAt DESC")
    List<Reservation> findByShowTimeWithDetailsOrderByCreatedAtDesc(@Param("showTime") ShowTime showTime);
    // ▲▲▲ 수정된 메소드 ▲▲▲
}
