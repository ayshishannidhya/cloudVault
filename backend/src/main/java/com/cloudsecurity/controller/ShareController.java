package com.cloudsecurity.controller;

import com.cloudsecurity.dto.ShareRequest;
import com.cloudsecurity.model.AuditLog;
import com.cloudsecurity.service.AuditLogService;
import com.cloudsecurity.service.ShareService;
import com.cloudsecurity.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    private final ShareService shareService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public ShareController(ShareService shareService,
                           AuditLogService auditLogService,
                           UserRepository userRepository) {
        this.shareService = shareService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    private String getUsername(Long userId) {
        return userRepository.findById(userId).map(u -> u.getUsername()).orElse("unknown");
    }

    /**
     * POST /api/share
     * Share a file with a recipient.
     */
    @PostMapping
    public ResponseEntity<String> shareFile(
            @Valid @RequestBody ShareRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) throws Exception {

        Long userId = (Long) auth.getPrincipal();
        String username = getUsername(userId);

        shareService.shareFile(request.getFileId(), userId, request.getRecipientUsername());

        auditLogService.log(AuditLog.Action.FILE_SHARE, username,
                request.getFileId(), null, request.getRecipientUsername(),
                "File shared with " + request.getRecipientUsername(),
                httpRequest.getRemoteAddr(), true);

        return ResponseEntity.ok("File shared successfully with " + request.getRecipientUsername());
    }

    /**
     * DELETE /api/share
     * Revoke a recipient's access to a file.
     */
    @DeleteMapping
    public ResponseEntity<String> revokeAccess(
            @Valid @RequestBody ShareRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        Long userId = (Long) auth.getPrincipal();
        String username = getUsername(userId);

        shareService.revokeAccess(request.getFileId(), userId, request.getRecipientUsername());

        auditLogService.log(AuditLog.Action.FILE_REVOKE, username,
                request.getFileId(), null, request.getRecipientUsername(),
                "Access revoked for " + request.getRecipientUsername(),
                httpRequest.getRemoteAddr(), true);

        return ResponseEntity.ok("Access revoked for " + request.getRecipientUsername());
    }

    /**
     * GET /api/share/{fileId}/users
     * List all users a file is shared with.
     */
    @GetMapping("/{fileId}/users")
    public ResponseEntity<List<Map<String, Object>>> getSharedUsers(
            @PathVariable Long fileId,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(shareService.getSharedUsers(fileId, userId));
    }
}
