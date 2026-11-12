package com.cloudsecurity.controller;

import com.cloudsecurity.dto.FileMetadataDto;
import com.cloudsecurity.dto.FileUploadResponse;
import com.cloudsecurity.model.AuditLog;
import com.cloudsecurity.service.AuditLogService;
import com.cloudsecurity.service.FileStorageService;
import com.cloudsecurity.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public FileController(FileStorageService fileStorageService,
                          AuditLogService auditLogService,
                          UserRepository userRepository) {
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    private String getUsername(Long userId) {
        return userRepository.findById(userId).map(u -> u.getUsername()).orElse("unknown");
    }

    /**
     * POST /api/files/upload
     * Upload a file — server encrypts it with AES-256-GCM and wraps the key.
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication auth,
            HttpServletRequest httpRequest) throws Exception {

        Long userId = (Long) auth.getPrincipal();
        FileUploadResponse response = fileStorageService.uploadFile(file, userId);

        auditLogService.log(AuditLog.Action.FILE_UPLOAD, getUsername(userId),
                response.getFileId(), file.getOriginalFilename(),
                "File uploaded and encrypted (" + file.getSize() + " bytes)",
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/files/my-files
     */
    @GetMapping("/my-files")
    public ResponseEntity<List<FileMetadataDto>> getMyFiles(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(fileStorageService.getMyFiles(userId));
    }

    /**
     * GET /api/files/shared-with-me
     */
    @GetMapping("/shared-with-me")
    public ResponseEntity<List<FileMetadataDto>> getSharedWithMe(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(fileStorageService.getSharedWithMe(userId));
    }

    /**
     * GET /api/files/download/{fileId}
     * Download an encrypted file + the user's encrypted AES key + IV.
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long fileId,
            Authentication auth,
            HttpServletRequest httpRequest) throws Exception {

        Long userId = (Long) auth.getPrincipal();
        String username = getUsername(userId);

        try {
            FileStorageService.DownloadResult result = fileStorageService.downloadFile(fileId, userId);

            auditLogService.log(AuditLog.Action.FILE_DOWNLOAD, username,
                    fileId, result.getFilename(),
                    "File downloaded by " + username,
                    httpRequest.getRemoteAddr());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFilename() + "\"")
                    .header("X-Encrypted-AES-Key", result.getEncryptedAesKey())
                    .header("X-IV", result.getIv())
                    .header("X-Original-Filename", result.getFilename())
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                            "X-Encrypted-AES-Key, X-IV, X-Original-Filename")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(result.getEncryptedData());
        } catch (Exception e) {
            auditLogService.log(AuditLog.Action.ACCESS_DENIED, username,
                    fileId, null, null,
                    "Download denied: " + e.getMessage(),
                    httpRequest.getRemoteAddr(), false);
            throw e;
        }
    }

    /**
     * DELETE /api/files/{fileId}
     * Delete a file (owner only).
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<String> deleteFile(
            @PathVariable Long fileId,
            Authentication auth,
            HttpServletRequest httpRequest) throws Exception {

        Long userId = (Long) auth.getPrincipal();
        String username = getUsername(userId);

        try {
            fileStorageService.deleteFile(fileId, userId);
            auditLogService.log(AuditLog.Action.FILE_DELETE, username,
                    fileId, null,
                    "File deleted by owner",
                    httpRequest.getRemoteAddr());
            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception e) {
            auditLogService.log(AuditLog.Action.ACCESS_DENIED, username,
                    fileId, null, null,
                    "Delete denied: " + e.getMessage(),
                    httpRequest.getRemoteAddr(), false);
            throw e;
        }
    }
}
