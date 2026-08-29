package com.cloudsecurity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Server RSA Key Configuration.
 * Generates or loads the server's RSA-2048 key pair on startup.
 * Keys are stored as Base64-encoded files on disk (portable, no certificate needed).
 */
@Configuration
public class ServerKeyConfig {

    @Value("${server.rsa.keystore-path}")
    private String keystorePath;

    private KeyPair serverKeyPair;

    /**
     * On application startup, load or generate the server's RSA-2048 key pair.
     */
    @PostConstruct
    public void init() throws Exception {
        Path keyDir = Paths.get(keystorePath).getParent();
        if (keyDir == null) {
            keyDir = Paths.get("./keystore");
        }
        Files.createDirectories(keyDir);

        Path publicKeyFile = keyDir.resolve("server_public.key");
        Path privateKeyFile = keyDir.resolve("server_private.key");

        if (Files.exists(publicKeyFile) && Files.exists(privateKeyFile)) {
            loadExistingKeyPair(publicKeyFile, privateKeyFile);
        } else {
            generateAndStoreKeyPair(publicKeyFile, privateKeyFile);
        }

        System.out.println("Server RSA key pair loaded successfully.");
    }

    /**
     * Load existing RSA key pair from files.
     */
    private void loadExistingKeyPair(Path publicKeyFile, Path privateKeyFile) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        // Load public key (X.509 / SPKI format)
        byte[] publicKeyBytes = Base64.getDecoder().decode(Files.readString(publicKeyFile).trim());
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        // Load private key (PKCS8 format)
        byte[] privateKeyBytes = Base64.getDecoder().decode(Files.readString(privateKeyFile).trim());
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        this.serverKeyPair = new KeyPair(publicKey, privateKey);
    }

    /**
     * Generate a new RSA-2048 key pair and store it in files.
     */
    private void generateAndStoreKeyPair(Path publicKeyFile, Path privateKeyFile) throws Exception {
        // Generate RSA-2048 key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        this.serverKeyPair = keyGen.generateKeyPair();

        // Save public key as Base64
        String publicKeyBase64 = Base64.getEncoder().encodeToString(
                serverKeyPair.getPublic().getEncoded());
        Files.writeString(publicKeyFile, publicKeyBase64);

        // Save private key as Base64
        String privateKeyBase64 = Base64.getEncoder().encodeToString(
                serverKeyPair.getPrivate().getEncoded());
        Files.writeString(privateKeyFile, privateKeyBase64);

        System.out.println("Generated new server RSA-2048 key pair.");
    }

    public PublicKey getServerPublicKey() {
        return serverKeyPair.getPublic();
    }

    public PrivateKey getServerPrivateKey() {
        return serverKeyPair.getPrivate();
    }
}
