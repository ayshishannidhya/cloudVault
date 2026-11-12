package com.cloudsecurity.service;

import com.cloudsecurity.model.AuditLog;
import com.cloudsecurity.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Audit Log Service — provides a centralized logging mechanism
 * for all security-relevant operations in CloudVault.
 *
 * Every file upload, download, delete, share, revoke, login,
 * and registration is recorded with timestamp, user, and IP address.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log a security-relevant action.
     */
    public void log(AuditLog.Action action, String username, Long fileId,
                    String filename, String targetUsername, String details,
                    String ipAddress, boolean success) {
        AuditLog entry = new AuditLog(
                action, username, fileId, filename,
                targetUsername, details, ipAddress, success
        );
        auditLogRepository.save(entry);
    }

    /**
     * Convenience: log a successful action without target user.
     */
    public void log(AuditLog.Action action, String username, Long fileId,
                    String filename, String details, String ipAddress) {
        log(action, username, fileId, filename, null, details, ipAddress, true);
    }

    /**
     * Convenience: log an auth action (no file involved).
     */
    public void logAuth(AuditLog.Action action, String username,
                        String details, String ipAddress, boolean success) {
        log(action, username, null, null, null, details, ipAddress, success);
    }

    /**
     * Get recent audit logs (last 50).
     */
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    /**
     * Get all logs for a specific file.
     */
    public List<AuditLog> getLogsByFile(Long fileId) {
        return auditLogRepository.findByFileIdOrderByTimestampDesc(fileId);
    }

    /**
     * Get all logs for a specific user.
     */
    public List<AuditLog> getLogsByUser(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }
}

