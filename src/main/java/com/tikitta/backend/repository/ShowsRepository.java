package com.tikitta.backend.repository;

import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.domain.Shows;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowsRepository extends JpaRepository<Shows, Long> {
    List<Shows> findByManager(Manager manager);
}
