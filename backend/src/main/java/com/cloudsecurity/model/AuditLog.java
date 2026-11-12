package com.cloudsecurity.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit Log — records every security-relevant action in the system.
 * Provides a tamper-evident trail of who did what, when, and to which file.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    public enum Action {
        USER_REGISTER,
        USER_LOGIN,
        FILE_UPLOAD,
        FILE_DOWNLOAD,
        FILE_DELETE,
        FILE_SHARE,
        FILE_REVOKE,
        ACCESS_DENIED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Action action;

    @Column(nullable = false)
    private String username;

    @Column
    private Long fileId;

    @Column
    private String filename;

    @Column
    private String targetUsername;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean success;

    public AuditLog() {}

    public AuditLog(Action action, String username, Long fileId, String filename,
                    String targetUsername, String details, String ipAddress, boolean success) {
        this.action = action;
        this.username = username;
        this.fileId = fileId;
        this.filename = filename;
        this.targetUsername = targetUsername;
        this.details = details;
        this.ipAddress = ipAddress;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Action getAction() { return action; }
    public String getUsername() { return username; }
    public Long getFileId() { return fileId; }
    public String getFilename() { return filename; }
    public String getTargetUsername() { return targetUsername; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSuccess() { return success; }
}

