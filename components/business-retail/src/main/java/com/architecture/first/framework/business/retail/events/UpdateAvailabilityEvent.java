package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.util.SimpleModel;

public class UpdateAvailabilityEvent extends ArchitectureFirstEvent {
    public UpdateAvailabilityEvent(Object source, String from, String to) {
        super(source, from, to);
    }

}
