package com.architecture.first.framework.security.events;

import com.architecture.first.framework.security.model.Token;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.List;

/**
 * Represents a request for an access token
 */
public class TokenRequestEvent extends ArchitectureFirstEvent implements AccessRequestEvent {
    public TokenRequestEvent(Object source, String from, List<String> to) {
        this(source, from, to, null);
    }

    public TokenRequestEvent(Object source, String from, List<String> to, ArchitectureFirstEvent originalEvent) {
        super(source, "TokenRequestEvent", from, to, originalEvent);
    }

    public TokenRequestEvent(Object source, String from, String to) {
        this(source, from, to, null);
    }

    public TokenRequestEvent(Object source, String from, String to, ArchitectureFirstEvent originalEvent) {
        super(source, "TokenRequestEvent", from, to, originalEvent);
    }

    public TokenRequestEvent setToken(Token token) {
        this.payload().put("token", token);
        return this;
    }

    public Token getToken() {
        return (Token) this.payload().get("token");
    }
}
