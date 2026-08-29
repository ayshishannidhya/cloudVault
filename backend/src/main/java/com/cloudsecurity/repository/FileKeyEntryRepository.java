package com.cloudsecurity.repository;

import com.cloudsecurity.model.FileKeyEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileKeyEntryRepository extends JpaRepository<FileKeyEntry, Long> {

    // Find a specific user's key entry for a file
    Optional<FileKeyEntry> findByFileIdAndUserIdAndKeyType(Long fileId, Long userId, FileKeyEntry.KeyType keyType);

    // Find the server's key entry for a file (used during sharing)
    Optional<FileKeyEntry> findByFileIdAndKeyType(Long fileId, FileKeyEntry.KeyType keyType);

    // Find all key entries for a file (to list shared users)
    List<FileKeyEntry> findByFileId(Long fileId);

    // Find all files shared with a user (SHARED type only)
    List<FileKeyEntry> findByUserIdAndKeyType(Long userId, FileKeyEntry.KeyType keyType);

    // Delete key entry (for revoking access)
    void deleteByFileIdAndUserIdAndKeyType(Long fileId, Long userId, FileKeyEntry.KeyType keyType);

    // Delete all key entries for a file (when deleting a file)
    void deleteByFileId(Long fileId);
}
