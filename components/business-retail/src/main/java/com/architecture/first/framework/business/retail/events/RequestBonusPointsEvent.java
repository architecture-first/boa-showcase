package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

public class RequestBonusPointsEvent extends ArchitectureFirstEvent {
    private Long customerId;
    private Long orderNumber;

    public RequestBonusPointsEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
    }
}
