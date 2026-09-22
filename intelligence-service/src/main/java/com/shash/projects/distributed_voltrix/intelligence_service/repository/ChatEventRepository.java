package com.shash.projects.distributed_voltrix.intelligence_service.repository;

import com.shash.projects.distributed_voltrix.intelligence_service.entity.ChatEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatEventRepository extends JpaRepository<ChatEvent, Long> {
}
