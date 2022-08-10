package com.architecture.first.framework.business.vicinity.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

/**
 * The event sent when an Actor enters a Vicinity
 */
public class ActorResumeEvent extends ArchitectureFirstEvent {

    public ActorResumeEvent(Object source, String from, String to) {
        super(source, "ActorResumeEvent", from, to);
    }

    /**
     * Set an access token
     * @param joinToken
     * @return
     */
    public ActorResumeEvent setAccessToken(String joinToken) {
        this.header().put("jwtToken", joinToken);
        return this;
    }

    /**
     * Returns an access token
     * @return
     */
    public String getAccessToken() {
        return (String) this.header().get("jwtToken");
    }

    /**
     * Sets a join token
     * @param joinToken
     * @return
     */
    public ActorResumeEvent setJoinToken(String joinToken) {
        this.payload().put("JOIN_TOKEN", joinToken);
        return this;
    }

    /**
     * Returns a join token
     * @return
     */
    public String getJoinToken() {
        return (String) this.payload().get("JOIN_TOKEN");
    }

    /**
     * Sets an override token
     * @param overrideToken
     * @return
     */
    public ActorResumeEvent setOverrideToken(String overrideToken) {
        this.payload().put("OVERRIDE_TOKEN", overrideToken);
        return this;
    }

    /**
     * Returns an override token
     * @return
     */
    public String getOverrideToken() {
        return (String) this.payload().get("OVERRIDE_TOKEN");
    }

}
