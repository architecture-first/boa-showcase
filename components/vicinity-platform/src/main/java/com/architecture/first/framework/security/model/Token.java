package com.architecture.first.framework.security.model;

/**
 * Represents an access token
 */
public class Token {
    private String requestId = "";
    private String token = "";

    public Token() {
    }

    public Token(String requestId) {
        this.requestId = requestId;
    }

    public Token(String requestId, String token) {
        this.requestId = requestId;
        this.token = token;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
