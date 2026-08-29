package com.cloudsecurity.controller;

import com.cloudsecurity.dto.FileMetadataDto;
import com.cloudsecurity.dto.FileUploadResponse;
import com.cloudsecurity.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * POST /api/files/upload
     * Upload a file — server encrypts it with AES-256-GCM and wraps the key.
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws Exception {

        Long userId = (Long) auth.getPrincipal();
        FileUploadResponse response = fileStorageService.uploadFile(file, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/files/my-files
     * List all files owned by the authenticated user.
     */
    @GetMapping("/my-files")
    public ResponseEntity<List<FileMetadataDto>> getMyFiles(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(fileStorageService.getMyFiles(userId));
    }

    /**
     * GET /api/files/shared-with-me
     * List all files shared with the authenticated user.
     */
    @GetMapping("/shared-with-me")
    public ResponseEntity<List<FileMetadataDto>> getSharedWithMe(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(fileStorageService.getSharedWithMe(userId));
    }

    /**
     * GET /api/files/download/{fileId}
     * Download an encrypted file + the user's encrypted AES key + IV.
     * The client will decrypt using their RSA private key → AES key → file.
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long fileId,
            Authentication auth) throws Exception {

        Long userId = (Long) auth.getPrincipal();
        FileStorageService.DownloadResult result = fileStorageService.downloadFile(fileId, userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFilename() + "\"")
                .header("X-Encrypted-AES-Key", result.getEncryptedAesKey())
                .header("X-IV", result.getIv())
                .header("X-Original-Filename", result.getFilename())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        "X-Encrypted-AES-Key, X-IV, X-Original-Filename")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(result.getEncryptedData());
    }

    /**
     * DELETE /api/files/{fileId}
     * Delete a file (owner only).
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<String> deleteFile(
            @PathVariable Long fileId,
            Authentication auth) throws Exception {

        Long userId = (Long) auth.getPrincipal();
        fileStorageService.deleteFile(fileId, userId);
        return ResponseEntity.ok("File deleted successfully");
    }
}
