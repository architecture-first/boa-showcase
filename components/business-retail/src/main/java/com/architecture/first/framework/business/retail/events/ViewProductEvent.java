package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.business.retail.model.cashier.model.inventory.Product;
import com.architecture.first.framework.business.retail.model.customer.cart.CartItem;
import com.architecture.first.framework.business.vicinity.events.AnonymousOkEvent;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.google.gson.Gson;

import java.util.List;

public class ViewProductEvent extends ArchitectureFirstEvent implements AnonymousOkEvent {
    public ViewProductEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public ArchitectureFirstEvent addProduct(Product product) {
        return this;
    }

    public ArchitectureFirstEvent addProducts(List<Product> products) {
          return this;
    }

    public Long getProductId() {
         return (Long) this.payload().get("productId");
    }

    public ViewProductEvent setProductId(Long productId) {
        this.payload().put("productId", productId);
        return this;
    }

    public CartItem getProduct() {
        var gson = new Gson();  // example of payload to pojo
        return gson.fromJson(gson.toJson(this.payload().get("product")), CartItem.class);
    }

    public ViewProductEvent setProduct(CartItem product) {
        this.payload().put("product", product);
        return this;
    }

    @Override
    public void onVicinityInit() {
        if (this.payload().get("productId") instanceof Double) {
            setProductId(((Double) this.payload().get("productId")).longValue());     // Convert Gson default data type
        }
    }

}
