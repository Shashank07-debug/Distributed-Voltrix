package com.shash.projects.distributed_voltrix.workspace_service.repository;

import com.shash.projects.distributed_voltrix.workspace_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
