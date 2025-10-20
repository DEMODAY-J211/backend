package com.tikitta.backend.repository;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.Reservation;
import com.tikitta.backend.domain.ShowTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {
    List<Reservation> findByShowTimeAndStatusIn(ShowTime showTime, List<DomainEnums.ReservationStatus> statuses);
}
