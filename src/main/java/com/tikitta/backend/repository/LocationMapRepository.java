package com.tikitta.backend.repository;

import com.tikitta.backend.domain.LocationMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationMapRepository extends JpaRepository<LocationMap, Long> {
    Optional<LocationMap> findByLocationId(Long locationId);
}