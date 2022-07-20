package com.architecture.first.framework.business.retail.model.customer.cart;

import com.architecture.first.framework.business.retail.model.cashier.model.inventory.Product;
import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.PurchaseItem;
import com.architecture.first.framework.security.model.SystemInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class CartItem extends SystemInfo {
    private Long productId;
    private String name;
    private String type;
    private String imageUrl;
    private List<String> attributes;
    private Integer quantity;
    private BigDecimal originalPrice;
    private BigDecimal calculatedPrice;
    private List<BigDecimal> discounts;
    
    public CartItem() {}
    public CartItem(PurchaseItem purchaseItem) {
        this.setProductId(purchaseItem.getProductId());
        this.setName(purchaseItem.getName());
        this.setType(purchaseItem.getType());
        this.setImageUrl(purchaseItem.getImageUrl());
        this.setAttributes(purchaseItem.getAttributes());
        this.setQuantity(0);
        this.setOriginalPrice(purchaseItem.getTotalUnitPrice());
        this.setCalculatedPrice(purchaseItem.getTotalUnitPrice());
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getCalculatedPrice() {
        return calculatedPrice;
    }

    public void setCalculatedPrice(BigDecimal calculatedPrice) {
        this.calculatedPrice = calculatedPrice;
    }

    public List<BigDecimal> getDiscounts() {
        return discounts;
    }

    public void setDiscounts(List<BigDecimal> discounts) {
        this.discounts = discounts;
    }

    public static CartItem from(Product product) {
        var item = new CartItem();
        item.setProductId(product.getProductId());
        item.setName(product.getName());
        item.setType(product.getType());
        item.setImageUrl(product.getImageUrl());
        item.setAttributes(product.getAttributes());
        item.setQuantity(0);
        item.setOriginalPrice(product.getPrice().get(0).getValue());

        LocalDate now = LocalDate.now();
        List<BigDecimal> discounts = product.getPrice().stream()
                .flatMap(p -> p.getDiscounts().stream()
                        .filter(d -> d.getEffectiveDate() == null
                                || (d.getEffectiveDate().compareTo(now) <= 0
                                        && (d.getExpirationDate() == null || d.getExpirationDate().compareTo(now) >= 0)))
                        .map(d -> d.getValue()))
                .collect(Collectors.toList());
        item.setDiscounts(discounts);

        BigDecimal calculatedPrice = discounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(new BigDecimal(-1))
                .add(item.getOriginalPrice());

        item.setCalculatedPrice(calculatedPrice);

        return item;
    }
    
    public static CartItem from(PurchaseItem purchaseItem) {
        return new CartItem(purchaseItem);
    }
}
