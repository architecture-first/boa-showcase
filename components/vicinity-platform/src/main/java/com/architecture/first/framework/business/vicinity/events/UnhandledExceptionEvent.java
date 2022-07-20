package com.architecture.first.framework.business.vicinity.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

/**
 * Represents an event sent when an exception was not handled by a participant
 */
public class UnhandledExceptionEvent extends ArchitectureFirstEvent implements ErrorEvent {

    public UnhandledExceptionEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public void setException(Throwable t) {
        this.payload().put("exception", t);
    }

    public Throwable getException() {
        return (Throwable) this.payload().get("exception");
    }

}
