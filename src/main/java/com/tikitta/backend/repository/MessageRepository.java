package com.tikitta.backend.repository;

import com.tikitta.backend.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message,Long> {
}
