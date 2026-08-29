package com.cloudsecurity.dto;

public class FileUploadResponse {

    private Long fileId;
    private String filename;
    private Long fileSize;
    private String message;
    private String encryptedAesKey; // Owner's RSA-encrypted AES key (Base64)
    private String iv;

    public FileUploadResponse() {}

    public FileUploadResponse(Long fileId, String filename, Long fileSize,
                               String encryptedAesKey, String iv, String message) {
        this.fileId = fileId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.encryptedAesKey = encryptedAesKey;
        this.iv = iv;
        this.message = message;
    }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEncryptedAesKey() { return encryptedAesKey; }
    public void setEncryptedAesKey(String encryptedAesKey) { this.encryptedAesKey = encryptedAesKey; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }
}
