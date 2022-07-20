package com.architecture.first.framework.technical.events;

import com.architecture.first.framework.business.actors.Actor;

/**
 * Represents an event that is used to check an Actor's health status
 */
public class SelfVicinityCheckupEvent extends ArchitectureFirstEvent implements CheckupEvent {

    public SelfVicinityCheckupEvent(Actor actor) {
        super(actor, actor.name(), actor.name());
    }
}
