package com.architecture.first.framework.business.vicinity.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

/**
 * Sent when a desired actor in a give group is not found
 */
public class ActorNotFoundEvent extends ArchitectureFirstEvent implements ErrorEvent {

    public ActorNotFoundEvent(Object source, String from, String to) {
        super(source, "ActorNotFoundEvent", from, to);
    }

    public ActorNotFoundEvent(Object source, String from, String to, ArchitectureFirstEvent event) {
        super(source, "ActorNotFoundEvent", from, to, event);
    }

    /**
     * Set the participant of the attempted conversation
     * @param participant
     * @return
     */
    public ActorNotFoundEvent setParticipant(String participant) {
        this.payload().put("participant", participant);
        return this;
    }

    /**
     * Returns the participant of the attempted conversation
     * @return
     */
    public String getParticipant() {
        return (String) this.payload().get("participant");
    }
}
