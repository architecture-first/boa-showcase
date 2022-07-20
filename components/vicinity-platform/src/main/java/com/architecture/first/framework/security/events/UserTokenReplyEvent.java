package com.architecture.first.framework.security.events;

import com.architecture.first.framework.security.model.UserToken;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.List;
import java.util.Map;

/**
 * Represents the reply for a user token request
 */
public class UserTokenReplyEvent extends TokenRequestEvent implements AccessRequestEvent {
    public UserTokenReplyEvent(Object source, String from, List<String> to) {
        this(source, from, to, null);
    }

    public UserTokenReplyEvent(Object source, String from, List<String> to, ArchitectureFirstEvent originalEvent) {
        super(source, from, to, originalEvent);
    }

    public UserTokenReplyEvent(Object source, String from, String to) {
        this(source, from, to, null);
    }

    public UserTokenReplyEvent(Object source, String from, String to, ArchitectureFirstEvent originalEvent) {
        super(source, from, to, originalEvent);
    }


    public UserTokenReplyEvent setCustomerToken(UserToken token) {
        this.payload().put("token", token);
        return this;
    }

    public UserToken getCustomerToken() {
        return UserToken.from((Map<String, Object>) this.payload().get("token"));
    }
}
