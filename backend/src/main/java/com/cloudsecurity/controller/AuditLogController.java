package com.cloudsecurity.controller;

import com.cloudsecurity.model.AuditLog;
import com.cloudsecurity.service.AuditLogService;
import com.cloudsecurity.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Audit Log Controller — provides read-only access to security audit logs.
 * Only authenticated users can view their own logs.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public AuditLogController(AuditLogService auditLogService, UserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/audit/my-logs
     * Get all audit logs for the authenticated user.
     */
    @GetMapping("/my-logs")
    public ResponseEntity<List<Map<String, Object>>> getMyLogs(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String username = userRepository.findById(userId)
                .map(u -> u.getUsername()).orElse("unknown");

        List<AuditLog> logs = auditLogService.getLogsByUser(username);
        return ResponseEntity.ok(logs.stream().map(this::toMap).collect(Collectors.toList()));
    }

    /**
     * GET /api/audit/file/{fileId}
     * Get all audit logs for a specific file.
     */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<List<Map<String, Object>>> getFileAuditLogs(
            @PathVariable Long fileId, Authentication auth) {

        List<AuditLog> logs = auditLogService.getLogsByFile(fileId);
        return ResponseEntity.ok(logs.stream().map(this::toMap).collect(Collectors.toList()));
    }

    /**
     * GET /api/audit/recent
     * Get the 50 most recent audit logs.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentLogs(Authentication auth) {
        List<AuditLog> logs = auditLogService.getRecentLogs();
        return ResponseEntity.ok(logs.stream().map(this::toMap).collect(Collectors.toList()));
    }

    private Map<String, Object> toMap(AuditLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("action", log.getAction().name());
        map.put("username", log.getUsername());
        map.put("fileId", log.getFileId());
        map.put("filename", log.getFilename());
        map.put("targetUsername", log.getTargetUsername());
        map.put("details", log.getDetails());
        map.put("ipAddress", log.getIpAddress());
        map.put("timestamp", log.getTimestamp().toString());
        map.put("success", log.isSuccess());
        return map;
    }
}

