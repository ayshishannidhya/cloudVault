package com.cloudsecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ShareRequest {

    @NotNull(message = "File ID is required")
    private Long fileId;

    @NotBlank(message = "Recipient username is required")
    private String recipientUsername;

    public ShareRequest() {}

    public ShareRequest(Long fileId, String recipientUsername) {
        this.fileId = fileId;
        this.recipientUsername = recipientUsername;
    }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getRecipientUsername() { return recipientUsername; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }
}
