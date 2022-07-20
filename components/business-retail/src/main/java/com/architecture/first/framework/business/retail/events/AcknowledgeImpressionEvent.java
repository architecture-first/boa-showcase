package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.util.SimpleModel;

public class AcknowledgeImpressionEvent extends ArchitectureFirstEvent {
    private SimpleModel crossSells = new SimpleModel();
    public AcknowledgeImpressionEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public SimpleModel getCrossSells() {
        return crossSells;
    }

    public ArchitectureFirstEvent setCrossSells(SimpleModel crossSells) {
        this.crossSells = crossSells;
        return this;
    }

    public ArchitectureFirstEvent addCrossSells(String name, Object data) {
        crossSells.put("data", data);
        return this;
    }
}
