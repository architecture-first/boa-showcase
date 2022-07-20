package com.architecture.first.framework.security.model;

/**
 * User Credentials
 */
public class Credentials {
    private String username;
    private String password;
    private static final String demoPassword = "";

    public Credentials() {}

    public Credentials(String userid, String password) {
        this.username = userid;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
