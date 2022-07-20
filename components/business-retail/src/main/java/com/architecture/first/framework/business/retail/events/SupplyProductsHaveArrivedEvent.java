package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SupplyProductsHaveArrivedEvent extends ArchitectureFirstEvent {
    private final Map<Long, Integer> productsThatArrived = new HashMap<>();

    public SupplyProductsHaveArrivedEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public ArchitectureFirstEvent addProduct(Long productId, Integer quantity) {
        if (productsThatArrived.containsKey(productId)) {
            quantity = productsThatArrived.get(productId) + quantity;
        }

        productsThatArrived.put(productId, quantity);
        return this;
    }

    public Map<Long, Integer> getProductsThatArrived() {
        return Collections.unmodifiableMap(productsThatArrived);
    }

}
