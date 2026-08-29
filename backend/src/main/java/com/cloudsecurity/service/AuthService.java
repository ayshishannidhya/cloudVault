package com.cloudsecurity.service;

import com.cloudsecurity.dto.AuthRequest;
import com.cloudsecurity.dto.AuthResponse;
import com.cloudsecurity.model.User;
import com.cloudsecurity.repository.UserRepository;
import com.cloudsecurity.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Register a new user.
     * The RSA public key is generated in the browser and sent during registration.
     */
    public AuthResponse register(AuthRequest request) {
        // Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (request.getRsaPublicKey() == null || request.getRsaPublicKey().isBlank()) {
            throw new RuntimeException("RSA public key is required for registration");
        }

        // Create user with hashed password
        User user = new User(
                request.getUsername(),
                request.getEmail() != null ? request.getEmail() : request.getUsername() + "@cloud.local",
                passwordEncoder.encode(request.getPassword()),
                request.getRsaPublicKey()
        );

        user = userRepository.save(user);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return AuthResponse.success(token, user.getUsername(), user.getId());
    }

    /**
     * Authenticate a user and return JWT token.
     */
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return AuthResponse.success(token, user.getUsername(), user.getId());
    }
}
