package com.tikitta.backend.repository;

import com.tikitta.backend.domain.LocationMap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationMapRepository extends JpaRepository<LocationMap, Long> {
}