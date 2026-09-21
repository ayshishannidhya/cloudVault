package com.cloudsecurity.service;

import com.cloudsecurity.dto.FileMetadataDto;
import com.cloudsecurity.dto.FileUploadResponse;
import com.cloudsecurity.model.EncryptedFile;
import com.cloudsecurity.model.FileKeyEntry;
import com.cloudsecurity.model.User;
import com.cloudsecurity.repository.EncryptedFileRepository;
import com.cloudsecurity.repository.FileKeyEntryRepository;
import com.cloudsecurity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * File Storage Service — handles encrypted file upload, download, and listing.
 *
 * Upload flow:
 * 1. Generate random AES-256 key + IV
 * 2. Encrypt file bytes with AES-256-GCM
 * 3. Encrypt AES key with owner's RSA public key → OWNER key entry
 * 4. Encrypt AES key with server's RSA public key → SERVER key entry
 * 5. Save encrypted file to disk, metadata + key entries to database
 */
@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final CryptoService cryptoService;
    private final KeyManagementService keyManagementService;
    private final EncryptedFileRepository fileRepository;
    private final FileKeyEntryRepository keyEntryRepository;
    private final UserRepository userRepository;

    public FileStorageService(CryptoService cryptoService,
                              KeyManagementService keyManagementService,
                              EncryptedFileRepository fileRepository,
                              FileKeyEntryRepository keyEntryRepository,
                              UserRepository userRepository) {
        this.cryptoService = cryptoService;
        this.keyManagementService = keyManagementService;
        this.fileRepository = fileRepository;
        this.keyEntryRepository = keyEntryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Upload and encrypt a file.
     */
    public FileUploadResponse uploadFile(MultipartFile file, Long userId) throws Exception {
        // Get user's RSA public key
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 1: Generate random AES key and IV
        SecretKey aesKey = cryptoService.generateAESKey();
        byte[] iv = cryptoService.generateIV();

        // Step 2: Encrypt file data with AES-256-GCM
        byte[] plainData = file.getBytes();
        byte[] encryptedData = cryptoService.encryptFileAES(plainData, aesKey, iv);

        // Step 3: Save encrypted file to disk
        String storedFilename = UUID.randomUUID().toString();
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(storedFilename);
        Files.write(filePath, encryptedData);

        // Step 4: Save file metadata to database
        String ivBase64 = cryptoService.encodeIV(iv);
        EncryptedFile encryptedFile = new EncryptedFile(
                file.getOriginalFilename(),
                storedFilename,
                file.getSize(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                ivBase64,
                userId
        );
        encryptedFile = fileRepository.save(encryptedFile);

        // Step 5: Encrypt AES key for owner (using owner's RSA public key)
        String ownerEncryptedKey = keyManagementService.wrapKeyForUser(aesKey, owner.getRsaPublicKey());
        keyEntryRepository.save(new FileKeyEntry(
                encryptedFile.getId(), userId, ownerEncryptedKey, FileKeyEntry.KeyType.OWNER
        ));

        // Step 6: Encrypt AES key for server (using server's RSA public key)
        String serverEncryptedKey = keyManagementService.wrapKeyForServer(aesKey);
        keyEntryRepository.save(new FileKeyEntry(
                encryptedFile.getId(), userId, serverEncryptedKey, FileKeyEntry.KeyType.SERVER
        ));

        return new FileUploadResponse(
                encryptedFile.getId(),
                file.getOriginalFilename(),
                file.getSize(),
                ownerEncryptedKey,
                ivBase64,
                "File uploaded and encrypted successfully"
        );
    }

    /**
     * Download an encrypted file (returns encrypted bytes + user's encrypted AES key).
     */
    public DownloadResult downloadFile(Long fileId, Long userId) throws Exception {
        // Get file metadata
        EncryptedFile encFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Determine key type based on whether user is owner or shared recipient
        FileKeyEntry.KeyType keyType = encFile.getOwnerId().equals(userId)
                ? FileKeyEntry.KeyType.OWNER
                : FileKeyEntry.KeyType.SHARED;

        // Get user's encrypted AES key
        FileKeyEntry keyEntry = keyEntryRepository.findByFileIdAndUserIdAndKeyType(fileId, userId, keyType)
                .orElseThrow(() -> new RuntimeException("Access denied: no key found for this user"));

        // Read encrypted file from disk
        Path filePath = Paths.get(uploadDir).resolve(encFile.getStoredFilename());
        byte[] encryptedData = Files.readAllBytes(filePath);

        return new DownloadResult(
                encryptedData,
                encFile.getOriginalFilename(),
                encFile.getContentType(),
                keyEntry.getEncryptedAesKey(),
                encFile.getIv()
        );
    }

    /**
     * List files owned by a user.
     */
    public List<FileMetadataDto> getMyFiles(Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<EncryptedFile> files = fileRepository.findByOwnerIdOrderByUploadedAtDesc(userId);
        List<FileMetadataDto> result = new ArrayList<>();

        for (EncryptedFile file : files) {
            FileMetadataDto dto = new FileMetadataDto();
            dto.setFileId(file.getId());
            dto.setOriginalFilename(file.getOriginalFilename());
            dto.setFileSize(file.getFileSize());
            dto.setContentType(file.getContentType());
            dto.setOwnerUsername(owner.getUsername());
            dto.setUploadedAt(file.getUploadedAt());
            dto.setOwner(true);
            dto.setIv(file.getIv());

            // Get owner's encrypted AES key
            keyEntryRepository.findByFileIdAndUserIdAndKeyType(file.getId(), userId, FileKeyEntry.KeyType.OWNER)
                    .ifPresent(entry -> dto.setEncryptedAesKey(entry.getEncryptedAesKey()));

            result.add(dto);
        }
        return result;
    }

    /**
     * List files shared with a user.
     */
    public List<FileMetadataDto> getSharedWithMe(Long userId) {
        List<FileKeyEntry> sharedEntries = keyEntryRepository.findByUserIdAndKeyType(userId, FileKeyEntry.KeyType.SHARED);
        List<FileMetadataDto> result = new ArrayList<>();

        for (FileKeyEntry entry : sharedEntries) {
            fileRepository.findById(entry.getFileId()).ifPresent(file -> {
                FileMetadataDto dto = new FileMetadataDto();
                dto.setFileId(file.getId());
                dto.setOriginalFilename(file.getOriginalFilename());
                dto.setFileSize(file.getFileSize());
                dto.setContentType(file.getContentType());
                dto.setUploadedAt(file.getUploadedAt());
                dto.setOwner(false);
                dto.setEncryptedAesKey(entry.getEncryptedAesKey());
                dto.setIv(file.getIv());

                // Get owner's username
                userRepository.findById(file.getOwnerId())
                        .ifPresent(owner -> dto.setOwnerUsername(owner.getUsername()));

                result.add(dto);
            });
        }
        return result;
    }

    /**
     * Delete a file (owner only).
     */
    @org.springframework.transaction.annotation.Transactional
    public void deleteFile(Long fileId, Long userId) throws IOException {
        EncryptedFile encFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!encFile.getOwnerId().equals(userId)) {
            throw new RuntimeException("Only the file owner can delete this file");
        }

        // Delete encrypted file from disk
        Path filePath = Paths.get(uploadDir).resolve(encFile.getStoredFilename());
        Files.deleteIfExists(filePath);

        // Delete all key entries
        keyEntryRepository.deleteByFileId(fileId);

        // Delete file metadata
        fileRepository.deleteById(fileId);
    }

    /**
     * Inner class for download result.
     */
    public static class DownloadResult {
        private final byte[] encryptedData;
        private final String filename;
        private final String contentType;
        private final String encryptedAesKey;
        private final String iv;

        public DownloadResult(byte[] encryptedData, String filename, String contentType,
                              String encryptedAesKey, String iv) {
            this.encryptedData = encryptedData;
            this.filename = filename;
            this.contentType = contentType;
            this.encryptedAesKey = encryptedAesKey;
            this.iv = iv;
        }

        public byte[] getEncryptedData() { return encryptedData; }
        public String getFilename() { return filename; }
        public String getContentType() { return contentType; }
        public String getEncryptedAesKey() { return encryptedAesKey; }
        public String getIv() { return iv; }
    }
}
