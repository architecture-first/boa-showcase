package com.architecture.first.merchant.controller;

import com.architecture.first.framework.business.retail.events.AcquireCrossSellProductsEvent;
import com.architecture.first.framework.business.retail.events.ViewProductEvent;
import com.architecture.first.framework.business.retail.model.customer.cart.CartItem;
import com.architecture.first.framework.business.retail.storefront.model.IProduct;
import com.architecture.first.framework.technical.events.DefaultLocalEvent;
import com.architecture.first.merchant.actors.Merchant;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.Product;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.ProductId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api/products")
public class MerchantController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private Merchant merchant;

    @GetMapping
    public List<? extends IProduct> getProducts() {
        return merchant.showProducts(new DefaultLocalEvent((String) request.getAttribute("requestId")));
    }

    @GetMapping("{productId}")
    public CartItem getProduct(@PathVariable("productId") Long id) {
        return merchant.showProduct(new ViewProductEvent(this, "MerchantController", "Merchant")
                .setProductId(id));
    }

    @GetMapping("/crosssell")
    public Object getCrossSells() {
        return merchant.showCrossSells(new DefaultLocalEvent((String) request.getAttribute("requestId")));
    }
}
