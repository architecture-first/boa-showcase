package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.business.retail.model.results.InventorySuggestedProductsResult;
import com.architecture.first.framework.business.vicinity.events.AnonymousOkEvent;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import java.util.*;

public class SuggestedProductsEvent extends ArchitectureFirstEvent implements AnonymousOkEvent {
    private final List<InventorySuggestedProductsResult> suggestedProducts = new ArrayList<>();

    public SuggestedProductsEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public ArchitectureFirstEvent addProduct(InventorySuggestedProductsResult product) {
        suggestedProducts.add(product);
        return this;
    }

    public ArchitectureFirstEvent addProducts(List<InventorySuggestedProductsResult> products) {
        suggestedProducts.addAll(products);
        return this;
    }

    public List<InventorySuggestedProductsResult> getSuggestedProducts() {
        return Collections.unmodifiableList(suggestedProducts);
    }

}
