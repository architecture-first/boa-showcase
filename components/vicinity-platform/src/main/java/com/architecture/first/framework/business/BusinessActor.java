package com.architecture.first.framework.business;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import org.springframework.context.event.EventListener;

public class BusinessActor extends Actor {
    @EventListener
    public void onArchitectureFirstEvent(ArchitectureFirstEvent event) {
        onApplicationEvent(this, event);
    }
}
