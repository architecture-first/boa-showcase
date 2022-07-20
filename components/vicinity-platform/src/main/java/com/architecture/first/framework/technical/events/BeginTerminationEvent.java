package com.architecture.first.framework.technical.events;

import com.architecture.first.framework.business.vicinity.events.ErrorEvent;

/**
 * The event sent to signal an Actor to terminate
 */
public class BeginTerminationEvent extends ArchitectureFirstEvent implements ErrorEvent {

    public BeginTerminationEvent(Object source, String from, String to) {
        super(source, from, to);
    }

}
