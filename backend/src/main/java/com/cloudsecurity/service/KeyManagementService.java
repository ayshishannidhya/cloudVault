package com.cloudsecurity.service;

import com.cloudsecurity.config.ServerKeyConfig;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.PublicKey;

/**
 * Key Management Service — handles all key wrapping/unwrapping operations.
 * This service uses the CryptoService for actual RSA operations and the
 * ServerKeyConfig for the server's RSA key pair.
 *
 * Key flow:
 * - Upload: AES key is wrapped for both the owner and the server
 * - Share: Server unwraps its copy, then wraps for the recipient
 * - Download: User receives their wrapped copy to decrypt client-side
 */
@Service
public class KeyManagementService {

    private final CryptoService cryptoService;
    private final ServerKeyConfig serverKeyConfig;

    public KeyManagementService(CryptoService cryptoService, ServerKeyConfig serverKeyConfig) {
        this.cryptoService = cryptoService;
        this.serverKeyConfig = serverKeyConfig;
    }

    /**
     * Encrypt AES key with a user's RSA public key.
     *
     * @param aesKey              The AES session key
     * @param userPublicKeyBase64 The user's RSA public key (Base64 SPKI format)
     * @return Base64-encoded encrypted AES key
     */
    public String wrapKeyForUser(SecretKey aesKey, String userPublicKeyBase64) throws Exception {
        PublicKey userPublicKey = cryptoService.decodePublicKey(userPublicKeyBase64);
        return cryptoService.encryptAESKeyWithRSA(aesKey, userPublicKey);
    }

    /**
     * Encrypt AES key with the server's RSA public key.
     * This copy is used by the server to facilitate sharing without involving the owner.
     *
     * @param aesKey The AES session key
     * @return Base64-encoded encrypted AES key (server-wrapped)
     */
    public String wrapKeyForServer(SecretKey aesKey) throws Exception {
        return cryptoService.encryptAESKeyWithRSA(aesKey, serverKeyConfig.getServerPublicKey());
    }

    /**
     * Decrypt the server-wrapped AES key using the server's RSA private key.
     * Used during sharing: server recovers the AES key to re-wrap it for the recipient.
     *
     * @param serverEncryptedKeyBase64 The server-encrypted AES key (Base64)
     * @return The recovered AES session key
     */
    public SecretKey unwrapServerKey(String serverEncryptedKeyBase64) throws Exception {
        return cryptoService.decryptAESKeyWithRSA(serverEncryptedKeyBase64,
                serverKeyConfig.getServerPrivateKey());
    }
}
