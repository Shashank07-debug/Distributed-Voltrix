package com.shash.projects.distributed_voltrix.intelligence_service.repository;

import com.shash.projects.distributed_voltrix.intelligence_service.entity.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {
    Optional<UsageLog> findByUserIdAndDate(Long userId, LocalDate today);
}
