package com.architecture.first.framework.business.vicinity.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

/**
 * Represents the results of a token that was rejected for various reasons
 */
public class InvalidTokenEvent extends ArchitectureFirstEvent implements ErrorEvent {

    public static final String REASON = "reason";

    public InvalidTokenEvent(Object source, String from, String to) {
        super(source, "InvalidTokenEvent", from, to);
    }

    public InvalidTokenEvent(Object source, ArchitectureFirstEvent eventToReplyTo) {
        super(source, eventToReplyTo);
    }

    public InvalidTokenEvent setReason(String reason) {
        this.payload().put(REASON, reason);
        return this;
    }

    public String getReason() {
        return (String) this.payload().get(REASON);
    }
}
