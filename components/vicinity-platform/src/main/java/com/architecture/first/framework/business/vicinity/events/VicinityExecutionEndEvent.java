package com.architecture.first.framework.business.vicinity.events;

import com.architecture.first.framework.business.actors.exceptions.ActorException;
import org.springframework.context.ApplicationEvent;

/**
 * Represents the end of a process executed in the Vicinity
 */
public class VicinityExecutionEndEvent extends ApplicationEvent {
    public enum Status {
        Succeeded ("Succeeded"),
        Canceled ("Canceled"),
        Failed ("Failed");

        private final String status;

        Status(String status) {
            this.status = status;
        }
    }

    private Status status;
    private ActorException exception;

    public VicinityExecutionEndEvent(Object source) {
        super(source);
    }

    public Status getStatus() {
        return status;
    }

    public VicinityExecutionEndEvent setStatus(Status status) {
        this.status = status;
        return this;
    }

    public ActorException getException() {
        return exception;
    }

    public VicinityExecutionEndEvent setException(ActorException exception) {
        this.exception = exception;
        return this;
    }
}
