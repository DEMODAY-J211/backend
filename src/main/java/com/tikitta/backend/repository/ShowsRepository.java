package com.tikitta.backend.repository;

import com.tikitta.backend.domain.Shows;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowsRepository extends JpaRepository<Shows, Long> {
}
