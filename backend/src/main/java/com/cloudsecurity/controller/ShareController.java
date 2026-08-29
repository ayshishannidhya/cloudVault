package com.cloudsecurity.controller;

import com.cloudsecurity.dto.ShareRequest;
import com.cloudsecurity.service.ShareService;
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

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * POST /api/share
     * Share a file with a recipient. The server handles all crypto — zero effort for owner.
     */
    @PostMapping
    public ResponseEntity<String> shareFile(
            @Valid @RequestBody ShareRequest request,
            Authentication auth) throws Exception {

        Long userId = (Long) auth.getPrincipal();
        shareService.shareFile(request.getFileId(), userId, request.getRecipientUsername());
        return ResponseEntity.ok("File shared successfully with " + request.getRecipientUsername());
    }

    /**
     * DELETE /api/share
     * Revoke a recipient's access to a file.
     */
    @DeleteMapping
    public ResponseEntity<String> revokeAccess(
            @Valid @RequestBody ShareRequest request,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        shareService.revokeAccess(request.getFileId(), userId, request.getRecipientUsername());
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
