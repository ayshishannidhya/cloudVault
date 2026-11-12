package com.cloudsecurity.controller;

import com.cloudsecurity.dto.AuthRequest;
import com.cloudsecurity.dto.AuthResponse;
import com.cloudsecurity.model.AuditLog;
import com.cloudsecurity.service.AuditLogService;
import com.cloudsecurity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditLogService auditLogService;

    public AuthController(AuthService authService, AuditLogService auditLogService) {
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    /**
     * POST /api/auth/register
     * Register a new user with username, password, email, and RSA public key.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request,
                                                  HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.register(request);
            auditLogService.logAuth(AuditLog.Action.USER_REGISTER, request.getUsername(),
                    "User registered successfully", httpRequest.getRemoteAddr(), true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            auditLogService.logAuth(AuditLog.Action.USER_REGISTER, request.getUsername(),
                    "Registration failed: " + e.getMessage(), httpRequest.getRemoteAddr(), false);
            throw e;
        }
    }

    /**
     * POST /api/auth/login
     * Authenticate user and return JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                               HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.login(request);
            auditLogService.logAuth(AuditLog.Action.USER_LOGIN, request.getUsername(),
                    "Login successful", httpRequest.getRemoteAddr(), true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            auditLogService.logAuth(AuditLog.Action.USER_LOGIN, request.getUsername(),
                    "Login failed: " + e.getMessage(), httpRequest.getRemoteAddr(), false);
            throw e;
        }
    }
}
