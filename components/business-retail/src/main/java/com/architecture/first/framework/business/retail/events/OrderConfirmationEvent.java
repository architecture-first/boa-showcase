package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation.OrderConfirmation;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

public class OrderConfirmationEvent extends ArchitectureFirstEvent {
    private OrderConfirmation orderConfirmation;

    public OrderConfirmationEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public Long getCustomerId() {
        return (Long) this.header().get("userId");
    }
    public Long getOrderNumber() { return orderConfirmation.getOrderNumber();}

    public OrderConfirmationEvent setCustomerId(Long customerId) {
        this.header().put("userId", customerId); return this;
    }

    public OrderConfirmation getOrderConfirmation() {
        return orderConfirmation;
    }

    public OrderConfirmationEvent setOrderConfirmation(OrderConfirmation orderConfirmation) {
        this.orderConfirmation = orderConfirmation;
        return this;
    }

    public static OrderConfirmationEvent fromForReplyWithoutPayload(String from, Object source, ArchitectureFirstEvent originalEvent) {
        OrderConfirmationEvent replyEvent = new OrderConfirmationEvent(source, from, originalEvent.from());
        replyEvent.setOriginalEvent(originalEvent);

        return replyEvent;
    }

    public static OrderConfirmationEvent fromForReply(String from, Object source, ArchitectureFirstEvent originalEvent) {
        OrderConfirmationEvent replyEvent = fromForReplyWithoutPayload(from, source, originalEvent);
        replyEvent.addPayload(originalEvent.payload());
        replyEvent.setAccessToken(originalEvent.getAccessToken());

        return replyEvent;
    }

    @Override
    public void onVicinityInit() {
        if (this.header().get("userId") instanceof Double) {
            setCustomerId(((Double) this.header().get("userId")).longValue());
        }
    }
}
