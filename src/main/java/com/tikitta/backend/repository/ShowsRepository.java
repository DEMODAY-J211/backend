package com.tikitta.backend.repository;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.domain.Shows;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShowsRepository extends JpaRepository<Shows, Long> {
    List<Shows> findByManager(Manager manager);
    List<Shows> findByManagerId(Long managerId);
    Optional<Shows> findByManagerAndStatus(Manager manager, DomainEnums.ShowStatus status);
}
