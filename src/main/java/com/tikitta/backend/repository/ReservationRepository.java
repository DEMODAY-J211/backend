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
            "LEFT JOIN FETCH r.reservationItems ri " + // LEFT JOIN: 아이템이 없을 수도 있으므로
            "LEFT JOIN FETCH ri.showSeat rss " +      // LEFT JOIN: 스탠딩은 showSeat이 null
            "LEFT JOIN FETCH rss.seat seat " +       // LEFT JOIN
            "WHERE r.id = :reservationId")
    Optional<Reservation> findByIdWithDetails(@Param("reservationId") Long reservationId);
}
