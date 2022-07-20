package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

public class ReviewInventoryEvent extends ArchitectureFirstEvent {
    public ReviewInventoryEvent(Object source, String from, String to) {
        super(source, from, to);
    }
}
