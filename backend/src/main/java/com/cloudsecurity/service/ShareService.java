package com.cloudsecurity.service;

import com.cloudsecurity.model.EncryptedFile;
import com.cloudsecurity.model.FileKeyEntry;
import com.cloudsecurity.model.User;
import com.cloudsecurity.repository.EncryptedFileRepository;
import com.cloudsecurity.repository.FileKeyEntryRepository;
import com.cloudsecurity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Share Service — handles zero-effort file sharing.
 *
 * Sharing flow (owner just clicks "Share"):
 * 1. Server retrieves its own encrypted copy of the AES key (SERVER key entry)
 * 2. Server decrypts the AES key using its RSA private key
 * 3. Server encrypts the AES key with the recipient's RSA public key
 * 4. Server stores the new key entry (SHARED type)
 *
 * The file owner does ZERO cryptographic work.
 */
@Service
public class ShareService {

    private final KeyManagementService keyManagementService;
    private final FileKeyEntryRepository keyEntryRepository;
    private final EncryptedFileRepository fileRepository;
    private final UserRepository userRepository;

    public ShareService(KeyManagementService keyManagementService,
                        FileKeyEntryRepository keyEntryRepository,
                        EncryptedFileRepository fileRepository,
                        UserRepository userRepository) {
        this.keyManagementService = keyManagementService;
        this.keyEntryRepository = keyEntryRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Share a file with a recipient. The server handles all cryptographic operations.
     */
    @Transactional
    public void shareFile(Long fileId, Long ownerUserId, String recipientUsername) throws Exception {
        // Validate ownership
        EncryptedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getOwnerId().equals(ownerUserId)) {
            throw new RuntimeException("Only the file owner can share this file");
        }

        // Find recipient
        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new RuntimeException("Recipient user not found: " + recipientUsername));

        if (recipient.getId().equals(ownerUserId)) {
            throw new RuntimeException("Cannot share a file with yourself");
        }

        // Check if already shared
        if (keyEntryRepository.findByFileIdAndUserIdAndKeyType(fileId, recipient.getId(),
                FileKeyEntry.KeyType.SHARED).isPresent()) {
            throw new RuntimeException("File is already shared with this user");
        }

        // Step 1: Get server's encrypted AES key
        FileKeyEntry serverKeyEntry = keyEntryRepository.findByFileIdAndKeyType(fileId, FileKeyEntry.KeyType.SERVER)
                .orElseThrow(() -> new RuntimeException("Server key entry not found — file may be corrupted"));

        // Step 2: Server decrypts AES key using its RSA private key
        SecretKey aesKey = keyManagementService.unwrapServerKey(serverKeyEntry.getEncryptedAesKey());

        // Step 3: Encrypt AES key with recipient's RSA public key
        String recipientEncryptedKey = keyManagementService.wrapKeyForUser(aesKey, recipient.getRsaPublicKey());

        // Step 4: Store the new key entry for the recipient
        keyEntryRepository.save(new FileKeyEntry(
                fileId, recipient.getId(), recipientEncryptedKey, FileKeyEntry.KeyType.SHARED
        ));
    }

    /**
     * Revoke a user's access to a shared file.
     */
    @Transactional
    public void revokeAccess(Long fileId, Long ownerUserId, String recipientUsername) {
        // Validate ownership
        EncryptedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getOwnerId().equals(ownerUserId)) {
            throw new RuntimeException("Only the file owner can revoke access");
        }

        // Find recipient
        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + recipientUsername));

        // Delete the shared key entry
        keyEntryRepository.deleteByFileIdAndUserIdAndKeyType(fileId, recipient.getId(),
                FileKeyEntry.KeyType.SHARED);
    }

    /**
     * Get list of users a file is shared with.
     */
    public List<Map<String, Object>> getSharedUsers(Long fileId, Long ownerUserId) {
        // Validate ownership
        EncryptedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getOwnerId().equals(ownerUserId)) {
            throw new RuntimeException("Only the file owner can view shared users");
        }

        List<FileKeyEntry> entries = keyEntryRepository.findByFileId(fileId);
        List<Map<String, Object>> sharedUsers = new ArrayList<>();

        for (FileKeyEntry entry : entries) {
            if (entry.getKeyType() == FileKeyEntry.KeyType.SHARED) {
                userRepository.findById(entry.getUserId()).ifPresent(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("userId", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    sharedUsers.add(userInfo);
                });
            }
        }

        return sharedUsers;
    }
}
