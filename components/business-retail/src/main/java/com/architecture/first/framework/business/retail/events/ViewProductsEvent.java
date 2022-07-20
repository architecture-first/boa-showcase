package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.business.retail.model.cashier.model.inventory.Product;
import com.architecture.first.framework.business.retail.model.criteria.ShowProductsCriteria;
import com.architecture.first.framework.business.vicinity.events.AnonymousOkEvent;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.events.DefaultLocalEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewProductsEvent extends ArchitectureFirstEvent implements AnonymousOkEvent {
    private final List<Product> products = new ArrayList<>();
    private ShowProductsCriteria criteria = new ShowProductsCriteria();

    public ViewProductsEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public ArchitectureFirstEvent addProduct(Product product) {
        products.add(product);
        return this;
    }

    public ArchitectureFirstEvent addProducts(List<Product> products) {
        this.products.addAll(products);
        return this;
    }

    public List<Product> getProducts() {
        return Collections.unmodifiableList(products);
    }

    public ShowProductsCriteria getCriteria() {
        return criteria;
    }

    public ViewProductsEvent setCriteria(ShowProductsCriteria criteria) {
        this.criteria = criteria;
        return this;
    }

    @Override
    public ViewProductsEvent initFromDefaultEvent(ArchitectureFirstEvent defaultLocalEvent) {
        return (ViewProductsEvent) super.initFromDefaultEvent(defaultLocalEvent);
    }
}
