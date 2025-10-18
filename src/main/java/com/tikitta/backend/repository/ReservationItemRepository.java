package com.tikitta.backend.repository;

import com.tikitta.backend.domain.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationItemRepository extends JpaRepository<ReservationItem,Long> {
}
