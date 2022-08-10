package com.architecture.first.framework.technical.events;

/**
 * An event sent when an Actor does not understand a received event
 */
public class ActorDidNotUnderstandEvent extends ArchitectureFirstEvent {
    private ArchitectureFirstEvent unansweredEvent;

    public ActorDidNotUnderstandEvent(Object source, String from, String to) {
        super(source, "ActorDidNotUnderstandEvent", from, to);
    }

    /**
     * Returns the event not understood
     * @return
     */
    public ArchitectureFirstEvent getEvent() {
         return unansweredEvent;
    }

    /**
     * Sets the misunderstood event
     * @param event
     * @return
     */
    public ActorDidNotUnderstandEvent setUnansweredEvent(ArchitectureFirstEvent event) {
        this.unansweredEvent = event;
        return this;
    }
}
