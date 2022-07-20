package com.architecture.first.framework.technical.events;

/**
 * An event used to start communication from an non-Actor
 */
public class DefaultLocalEvent extends ArchitectureFirstEvent implements LocalEvent {

    /**
     * Create a default event
     * @param requestId
     */
    public DefaultLocalEvent(String requestId) {
        super("default", "default", "default");
        setRequestId(requestId);
    }

    /**
     * Sets the event as local so it is not sent out into the Vicinity
     * @param status
     * @return
     */
    public DefaultLocalEvent setAsLocal(boolean status) {
        // do not allow overriding
        return this;
    }
}
