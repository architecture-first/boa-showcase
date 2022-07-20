package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.Order;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.List;
import java.util.Map;

public class RequestPaymentEvent extends ArchitectureFirstEvent {
    public RequestPaymentEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public Long getCustomerId() {
        return (Long) this.header().get("userId");
    }
    public Long getOrderNumber() {
        return getOrderPreview().getOrderNumber();
    }

    public RequestPaymentEvent setCustomerId(Long customerId) {
        this.header().put("userId", customerId); return this;
    }

    public Order getOrderPreview() {
        return Order.from((Map<String,Object>) payload().get("orderPreview"));
    }

    public RequestPaymentEvent setOrderPreview(Order orderPreview) {
        this.payload().put("orderPreview", orderPreview);
        return this;
    }

    @Override
    public void onVicinityInit() {
        if (this.header().get("userId") instanceof Double) {
            setCustomerId(((Double) this.header().get("userId")).longValue());     // Convert Gson default data type
        }
    }

}
