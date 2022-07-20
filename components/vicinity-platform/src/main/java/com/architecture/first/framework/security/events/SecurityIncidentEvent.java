package com.architecture.first.framework.security.events;

import com.architecture.first.framework.business.vicinity.events.ErrorEvent;
import com.architecture.first.framework.security.model.UserToken;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.List;

/**
 * Represents a security incident, such as an expired token
 */
public class SecurityIncidentEvent extends ArchitectureFirstEvent implements ErrorEvent {
    public SecurityIncidentEvent(Object source, String from, List<String> to) {
        super(source, from, to);
    }

    public SecurityIncidentEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public SecurityIncidentEvent(Object source, String from, String to, ArchitectureFirstEvent originalEvent) {
        super(source, from, to, originalEvent);
    }

    public SecurityIncidentEvent setCustomerToken(UserToken token) {
        this.payload().put("token", token);
        return this;
    }
}
