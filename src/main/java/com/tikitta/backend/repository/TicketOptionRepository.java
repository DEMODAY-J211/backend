package com.tikitta.backend.repository;

import com.tikitta.backend.domain.TicketOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketOptionRepository extends JpaRepository<TicketOption,Long> {
}
