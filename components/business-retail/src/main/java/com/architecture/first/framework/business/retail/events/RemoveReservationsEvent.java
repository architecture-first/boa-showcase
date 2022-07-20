package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RemoveReservationsEvent extends ArchitectureFirstEvent {
    private final Map<Long, Integer> productReservationsToRemove = new HashMap<>();

    public RemoveReservationsEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public ArchitectureFirstEvent addProductReservation(Long productId, Integer quantity) {
        if (productReservationsToRemove.containsKey(productId)) {
            quantity = productReservationsToRemove.get(productId) + quantity;
        }

        productReservationsToRemove.put(productId, quantity);
        return this;
    }

    public Map<Long, Integer> getProductReservationsToRemove() {
        return Collections.unmodifiableMap(productReservationsToRemove);
    }

}
