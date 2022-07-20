package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

public class NoProductsAvailableEvent extends ArchitectureFirstEvent {
    public NoProductsAvailableEvent(Object source, String from, String to) {
        super(source, from, to);
        String msg = "no products available based on criteria";
        payload().put("findings", msg);
        setMessage(msg);
    }
}
