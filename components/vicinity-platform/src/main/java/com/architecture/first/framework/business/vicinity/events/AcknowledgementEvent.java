package com.architecture.first.framework.business.vicinity.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

/**
 * Represents an acknowledgement that a previous event has been received
 */
public class AcknowledgementEvent extends ArchitectureFirstEvent {
    private String acknowledgedEventName;
    private ArchitectureFirstEvent acknowledgedEvent;

    public AcknowledgementEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    /**
     * Set the event that is acknowledged
     * @param event
     * @return this
     */
    public AcknowledgementEvent setAcknowledgementEvent(ArchitectureFirstEvent event) {
        this.acknowledgedEvent = event;
        this.acknowledgedEventName = event.name();
        this.setOriginalEvent(event);

        return this;
    }

    /**
     * Returns the acknowledged event
     * @return
     */
    public ArchitectureFirstEvent getAcknowledgedEvent() {return this.acknowledgedEvent;}

    /**
     * Returns the name of the acknowledged event
     * @return
     */
    public String getAcknowledgedEventName() {
        return acknowledgedEventName;
    }

    /**
     * Set the name of the acknowledged event
     * @param acknowledgedEventName
     * @return
     */
    public AcknowledgementEvent setAcknowledgedEventName(String acknowledgedEventName) {
        this.acknowledgedEventName = acknowledgedEventName;
        return this;
    }

    @Override
    public boolean requiresAcknowledgement() { return acknowledgedEvent.requiresAcknowledgement();}
}
