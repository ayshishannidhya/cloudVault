package com.cloudsecurity.model;

import jakarta.persistence.*;

@Entity
@Table(name = "file_key_entries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"fileId", "userId", "keyType"}))
public class FileKeyEntry {

    public enum KeyType {
        OWNER,      // Encrypted with owner's RSA public key
        SERVER,     // Encrypted with server's RSA public key (for sharing)
        SHARED      // Encrypted with recipient's RSA public key
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fileId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedAesKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KeyType keyType;

    public FileKeyEntry() {}

    public FileKeyEntry(Long fileId, Long userId, String encryptedAesKey, KeyType keyType) {
        this.fileId = fileId;
        this.userId = userId;
        this.encryptedAesKey = encryptedAesKey;
        this.keyType = keyType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEncryptedAesKey() { return encryptedAesKey; }
    public void setEncryptedAesKey(String encryptedAesKey) { this.encryptedAesKey = encryptedAesKey; }

    public KeyType getKeyType() { return keyType; }
    public void setKeyType(KeyType keyType) { this.keyType = keyType; }
}
