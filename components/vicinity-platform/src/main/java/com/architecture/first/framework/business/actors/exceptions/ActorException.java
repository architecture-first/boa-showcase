package com.architecture.first.framework.business.actors.exceptions;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

/**
 * The base exception for an Actor
 */
public class ActorException extends RuntimeException {
    private final Actor actor;
    private ArchitectureFirstEvent errorEvent;

    public ActorException(Actor actor, Exception e) {
        super(e);
        this.actor = actor;
    }

    public ActorException(Actor actor, ArchitectureFirstEvent errorEvent) {
        super(new RuntimeException());
        this.actor = actor;
        this.errorEvent = errorEvent;
    }

    public ActorException(Actor actor, String message) {
        super(message);
        this.actor = actor;
    }

    public Actor getActor() {return this.actor;}

    public ArchitectureFirstEvent getErrorEvent() {
        return errorEvent;
    }
}
