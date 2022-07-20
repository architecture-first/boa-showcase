package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.Order;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

public class PaymentFailureEvent extends ArchitectureFirstEvent {
    public PaymentFailureEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public Long getCustomerId() {
        return (Long) header().get("userId");
    }

    public PaymentFailureEvent setCustomerId(Long customerId) {
        this.header().put("userId", customerId); return this;
    }

    public Order getOrderConfirmation() {
        return (Order) header().get("orderConfirmation");
    }

    public PaymentFailureEvent setOrderConfirmation(Order orderConfirmation) {
        this.header().put("userId", orderConfirmation);
        return this;
    }

    public static PaymentFailureEvent fromForReplyWithoutPayload(String from, Object source, ArchitectureFirstEvent originalEvent) {
        PaymentFailureEvent replyEvent = new PaymentFailureEvent(source, from, originalEvent.from());
        replyEvent.setOriginalEvent(originalEvent);

        return replyEvent;
    }

    public static PaymentFailureEvent fromForReply(String from, Object source, ArchitectureFirstEvent originalEvent) {
        PaymentFailureEvent replyEvent = fromForReplyWithoutPayload(from, source, originalEvent);
        replyEvent.addPayload(originalEvent.payload());
        replyEvent.setAccessToken(originalEvent.getAccessToken());

        return replyEvent;
    }
}
