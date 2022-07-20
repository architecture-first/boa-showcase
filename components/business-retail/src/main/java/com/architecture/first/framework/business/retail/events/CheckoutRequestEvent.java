package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.Order;
import com.architecture.first.framework.business.retail.model.customer.cart.ShoppingCart;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.List;

public class CheckoutRequestEvent extends ArchitectureFirstEvent {
    private ShoppingCart shoppingCart;

    public CheckoutRequestEvent(Object source, String from, List<String> to) {
        super(source, from, to);
    }

    public Long getCustomerId() {
        return (Long) this.header().get("userId");
    }

    public CheckoutRequestEvent setCustomerId(Long customerId) {
        this.header().put("userId", customerId); return this;
    }

    public Long getOrderNumber() {
        return (Long) this.payload().get("orderNumber");
    }

    public CheckoutRequestEvent setOrderNumber(Long orderNumber) {
        this.payload().put("orderNumber", orderNumber); return this;
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCart;
    }

    public CheckoutRequestEvent setShoppingCart(ShoppingCart shoppingCart) {
        this.shoppingCart = shoppingCart;
        return this;
    }

    public Order getOrderPreview() {
        return (Order) payload().get("orderPreview");
    }

    public CheckoutRequestEvent setOrderPreview(Order orderPreview) {
        this.payload().put("orderPreview", orderPreview);
        return this;
    }

    @Override
    public void onVicinityInit() {
        if (this.payload().get("orderNumber") instanceof Double) {
            setOrderNumber(((Double) this.payload().get("orderNumber")).longValue());     // Convert Gson default data type
        }
        if (this.header().get("userId") instanceof Double) {
            setCustomerId(((Double) this.header().get("userId")).longValue());
        }
    }
}
