package com.architecture.first.framework.technical.events;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.vicinity.events.ErrorEvent;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;

/**
 * Represents an error during processing of an event
 */
public class ActorProcessingErrorEvent extends ArchitectureFirstEvent implements ErrorEvent {
    private ArchitectureFirstEvent erroredEvent;
    private transient RuntimeException exception;
    private String exceptionMessage;

    public ActorProcessingErrorEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    /**
     * Returns the event in error
     * @return
     */
    public ArchitectureFirstEvent getEvent() {
         return erroredEvent;
    }

    /**
     * Sets the event in error
     * @param event
     * @return
     */
    public ActorProcessingErrorEvent setErroredEvent(ArchitectureFirstEvent event) {
        this.erroredEvent = event;
        return this;
    }

    /**
     * Sets an exception
     * @param exception
     * @return
     */
    public ActorProcessingErrorEvent setException(RuntimeException exception) {
        this.exception = exception;
        exceptionMessage = exception.getMessage();
        return this;
    }

    /**
     * Returns the error message
     * @return
     */
     public String message() {
        var list = new ArrayList<String>();

        var friendlyMessage = getMessage();

        if (StringUtils.isNotEmpty(friendlyMessage)) {
            list.add(friendlyMessage);
        }
        if (StringUtils.isNotEmpty(exceptionMessage)) {
            list.add(exceptionMessage);
        }

        return String.join("; ", list);
    }

    public static ActorProcessingErrorEvent from(Actor source, ArchitectureFirstEvent fromEvent, RuntimeException exception) {
        var evt = new ActorProcessingErrorEvent(source, source.name(), fromEvent.from())
                .setErroredEvent(fromEvent)
                .setException(exception)
                .setOriginalEvent(fromEvent);

        return (ActorProcessingErrorEvent) evt;
    }
}
