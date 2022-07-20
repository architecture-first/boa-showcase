package com.architecture.first.framework.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Represents a user token
 */
@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class UserToken extends Token {
    public static final String APPROVED = "approved";
    public static final String VALIDATED = "validated";
    public static final String REJECTED = "rejected";
    private Long userId = null;
    private String firstName = "";
    private String status = "";
    private String reason = "";

    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public Long getUserId() {return userId;}
    public boolean isIdentityFound() {
        return getUserId() != null;
    }

    public UserToken approve(String jwtToken) {
        status = APPROVED;
        this.setToken(jwtToken);
        return this;
    }

    public UserToken validated() {
        status = VALIDATED;
        return this;
    }

    public UserToken reject(String reason) {
        status = REJECTED;
        this.reason = reason;
        return this;
    }

    public boolean isValidated() {
        return VALIDATED.equals(status);
    }

    public boolean isApproved() {
        return APPROVED.equals(status);
    }

    public boolean isRejected() {
        return REJECTED.equals(status);
    }

    public static UserToken from(Map<String, Object>  data) {
        UserToken userToken = new UserToken();
        userToken.setFirstName((String) data.get("firstName"));
        if (data.get("userId") != null) {
            userToken.setUserId(data.get("userId") instanceof Double ?
                    ((Double) data.get("userId")).longValue()
                    : (Long) data.get("userId"));
        }
        userToken.setToken((String) data.get("token"));
        userToken.setStatus((String) data.get("status"));
        userToken.setReason((String) data.get("reason"));
        return userToken;
    }

}
