package com.cloudsecurity.repository;

import com.cloudsecurity.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Get all logs for a specific user (ordered by newest first)
    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);

    // Get all logs for a specific file
    List<AuditLog> findByFileIdOrderByTimestampDesc(Long fileId);

    // Get all logs by action type
    List<AuditLog> findByActionOrderByTimestampDesc(AuditLog.Action action);

    // Get recent logs (top N)
    List<AuditLog> findTop50ByOrderByTimestampDesc();

    // Get logs within a time range
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}

