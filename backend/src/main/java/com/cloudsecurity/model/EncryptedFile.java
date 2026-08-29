package com.cloudsecurity.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "encrypted_files")
public class EncryptedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false, unique = true)
    private String storedFilename;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String iv;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    public EncryptedFile() {}

    public EncryptedFile(String originalFilename, String storedFilename, Long fileSize,
                         String contentType, String iv, Long ownerId) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.iv = iv;
        this.ownerId = ownerId;
        this.uploadedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
