package com.architecture.first.framework.business.vicinity.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

/**
 * Represents an event where the other participant in a conversation is no longer available
 */
public class ConversationBrokenEvent extends ArchitectureFirstEvent implements ErrorEvent {

    public ConversationBrokenEvent(Object source, String from, String to) {
        super(source, "ConversationBrokenEvent", from, to);
    }

    public ConversationBrokenEvent setParticipant(String participant) {
        this.payload().put("participant", participant);
        return this;
    }

    public String getParticipant() {
        return (String) this.payload().get("participant");
    }
}
