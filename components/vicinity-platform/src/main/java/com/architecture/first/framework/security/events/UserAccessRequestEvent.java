package com.architecture.first.framework.security.events;

import com.architecture.first.framework.security.model.Credentials;

import java.util.List;

/**
 * Represents a request for an access token using User credentials
 */
public class UserAccessRequestEvent extends TokenRequestEvent implements AccessRequestEvent {
    public UserAccessRequestEvent(Object source, String from, List<String> to) {
        super(source, from, to);
    }

    public UserAccessRequestEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public UserAccessRequestEvent setCredentials(Credentials credentials) {
        this.payload().put("credentials", credentials);
        return this;
    }

    public Credentials getCredentials() {
        return (Credentials) this.payload().get("credentials");
    }
}
