package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

public class EarnedBonusPointsEvent extends ArchitectureFirstEvent {
    private Long customerId;
    private Long orderNumber;

    public EarnedBonusPointsEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public Long getCustomerId() {
        return customerId;
    }

    public EarnedBonusPointsEvent setCustomerId(Long customerId) {
        this.customerId = customerId;
        return this;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public EarnedBonusPointsEvent setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }
}
