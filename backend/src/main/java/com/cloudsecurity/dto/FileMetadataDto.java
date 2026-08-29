package com.cloudsecurity.dto;

import java.time.LocalDateTime;

public class FileMetadataDto {

    private Long fileId;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private String ownerUsername;
    private LocalDateTime uploadedAt;
    private boolean isOwner;
    private String encryptedAesKey; // User's RSA-encrypted AES key (Base64)
    private String iv;

    public FileMetadataDto() {}

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public boolean isOwner() { return isOwner; }
    public void setOwner(boolean owner) { isOwner = owner; }

    public String getEncryptedAesKey() { return encryptedAesKey; }
    public void setEncryptedAesKey(String encryptedAesKey) { this.encryptedAesKey = encryptedAesKey; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }
}
