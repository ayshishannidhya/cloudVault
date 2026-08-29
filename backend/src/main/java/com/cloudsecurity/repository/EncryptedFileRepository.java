package com.cloudsecurity.repository;

import com.cloudsecurity.model.EncryptedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EncryptedFileRepository extends JpaRepository<EncryptedFile, Long> {
    List<EncryptedFile> findByOwnerIdOrderByUploadedAtDesc(Long ownerId);
}
