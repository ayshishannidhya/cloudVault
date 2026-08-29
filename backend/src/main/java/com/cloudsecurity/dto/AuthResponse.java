package com.cloudsecurity.dto;

public class AuthResponse {

    private String token;
    private String username;
    private Long userId;
    private String message;

    public AuthResponse() {}

    public AuthResponse(String token, String username, Long userId, String message) {
        this.token = token;
        this.username = username;
        this.userId = userId;
        this.message = message;
    }

    public static AuthResponse success(String token, String username, Long userId) {
        return new AuthResponse(token, username, userId, "Success");
    }

    public static AuthResponse message(String message) {
        return new AuthResponse(null, null, null, message);
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
