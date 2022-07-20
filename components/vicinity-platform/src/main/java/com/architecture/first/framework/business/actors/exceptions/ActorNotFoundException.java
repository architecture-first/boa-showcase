package com.architecture.first.framework.business.actors.exceptions;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

/**
 * Exception thrown when no Actor can be found to send an event to
 */
public class ActorNotFoundException extends ActorException {
    private ArchitectureFirstEvent event;

    public ActorNotFoundException(String message) {
        super(null, message);
    }

    public ArchitectureFirstEvent getEvent() {
        return event;
    }

    public ActorNotFoundException setEvent(ArchitectureFirstEvent event) {
        this.event = event;
        return this;
    }
}
