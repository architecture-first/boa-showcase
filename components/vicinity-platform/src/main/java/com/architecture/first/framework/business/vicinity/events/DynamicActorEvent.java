package com.architecture.first.framework.business.vicinity.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

/**
 * Represents an event sent to and from Dynamic Actors
 */
public class DynamicActorEvent extends ArchitectureFirstEvent implements ErrorEvent {

    public DynamicActorEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public DynamicActorEvent(Object source, String from, String to, ArchitectureFirstEvent event) {
        super(source, from, to, event);
    }
}
