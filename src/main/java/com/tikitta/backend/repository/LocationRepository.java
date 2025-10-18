package com.tikitta.backend.repository;

import com.tikitta.backend.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
