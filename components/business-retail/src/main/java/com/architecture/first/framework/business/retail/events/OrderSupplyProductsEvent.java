package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.HashMap;
import java.util.Map;

public class OrderSupplyProductsEvent extends ArchitectureFirstEvent {
    private final Map<Long, Integer> productsToOrder = new HashMap<>();

    public OrderSupplyProductsEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public ArchitectureFirstEvent addProduct(Long productId, Integer quantity) {
        if (productsToOrder.containsKey(productId)) {
            quantity = productsToOrder.get(productId) + quantity;
        }

        productsToOrder.put(productId, quantity);
        return this;
    }
}
