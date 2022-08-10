package com.architecture.first.framework.security;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.security.events.SecurityHolderEvent;
import org.springframework.context.event.EventListener;

public class SecurityActor extends Actor {

    @EventListener
    public void onSecurityHolderEvent(SecurityHolderEvent event) {
        onApplicationEvent(this, event.getArchitectureFirstEvent());
    }
}
