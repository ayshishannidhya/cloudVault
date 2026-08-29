package com.cloudsecurity.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Core Encryption Service — implements Hybrid Encryption using:
 * - AES-256-GCM for file data encryption (symmetric, fast)
 * - RSA-2048-OAEP for AES key encryption (asymmetric, secure key exchange)
 *
 * AES-GCM provides both confidentiality and integrity (authenticated encryption).
 */
@Service
public class CryptoService {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;      // bits
    private static final int GCM_TAG_LENGTH = 128;    // bits
    private static final int GCM_IV_LENGTH = 12;      // bytes (96 bits, recommended for GCM)

    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    // ==========================================
    // AES Key Generation
    // ==========================================

    /**
     * Generate a random AES-256 session key.
     */
    public SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE, SecureRandom.getInstanceStrong());
        return keyGen.generateKey();
    }

    /**
     * Generate a random Initialization Vector (IV) for AES-GCM.
     */
    public byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // ==========================================
    // AES Encryption / Decryption (File Data)
    // ==========================================

    /**
     * Encrypt file data using AES-256-GCM.
     *
     * @param plainData The original file bytes
     * @param aesKey    The AES session key
     * @param iv        The initialization vector
     * @return Encrypted bytes (ciphertext + GCM authentication tag)
     */
    public byte[] encryptFileAES(byte[] plainData, SecretKey aesKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        return cipher.doFinal(plainData);
    }

    /**
     * Decrypt file data using AES-256-GCM.
     *
     * @param encryptedData The encrypted bytes
     * @param aesKey        The AES session key
     * @param iv            The initialization vector used during encryption
     * @return Original file bytes
     */
    public byte[] decryptFileAES(byte[] encryptedData, SecretKey aesKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        return cipher.doFinal(encryptedData);
    }

    // ==========================================
    // RSA Encryption / Decryption (AES Key)
    // ==========================================

    /**
     * Encrypt an AES key using an RSA public key (RSA-OAEP).
     * Used for: wrapping the AES key for a specific user or the server.
     *
     * @param aesKey    The AES session key to encrypt
     * @param rsaPublicKey The RSA public key to encrypt with
     * @return Base64-encoded encrypted AES key
     */
    public String encryptAESKeyWithRSA(SecretKey aesKey, PublicKey rsaPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, javax.crypto.spec.PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey, oaepParams);
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    /**
     * Decrypt an RSA-encrypted AES key using an RSA private key.
     * Used for: server-side key recovery during sharing.
     *
     * @param encryptedAesKeyBase64 Base64-encoded RSA-encrypted AES key
     * @param rsaPrivateKey         The RSA private key to decrypt with
     * @return The recovered AES session key
     */
    public SecretKey decryptAESKeyWithRSA(String encryptedAesKeyBase64, PrivateKey rsaPrivateKey) throws Exception {
        byte[] encryptedKey = Base64.getDecoder().decode(encryptedAesKeyBase64);
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, javax.crypto.spec.PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey, oaepParams);
        byte[] aesKeyBytes = cipher.doFinal(encryptedKey);
        return new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
    }

    // ==========================================
    // Utility Methods
    // ==========================================

    /**
     * Convert a Base64-encoded RSA public key (SPKI format) to a PublicKey object.
     * The frontend sends the public key in this format.
     */
    public PublicKey decodePublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    /**
     * Encode IV bytes to Base64 string for storage.
     */
    public String encodeIV(byte[] iv) {
        return Base64.getEncoder().encodeToString(iv);
    }

    /**
     * Decode Base64-encoded IV string back to bytes.
     */
    public byte[] decodeIV(String ivBase64) {
        return Base64.getDecoder().decode(ivBase64);
    }
}
