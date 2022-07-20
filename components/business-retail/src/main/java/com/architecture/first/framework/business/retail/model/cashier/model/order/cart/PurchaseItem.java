package com.architecture.first.framework.business.retail.model.cashier.model.order.cart;

import com.architecture.first.framework.business.retail.model.customer.cart.CartItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class PurchaseItem {
    private Long productId;
    private Integer quantity;
    private BigDecimal totalUnitPrice;
    private String name;
    private String type;
    private String imageUrl;
    private List<String> attributes;

    public static PurchaseItem from(CartItem cartItem) {
        var pi = new PurchaseItem();
        pi.productId = cartItem.getProductId();
        pi.quantity = cartItem.getQuantity();
        pi.name = cartItem.getName();
        pi.imageUrl = cartItem.getImageUrl();
        pi.type = cartItem.getType();
        pi.totalUnitPrice = cartItem.getCalculatedPrice();

        return pi;
    }

    public static PurchaseItem from(Map<String,Object> data) { // handle marshalling due to Gson
        var pi = new PurchaseItem();
        pi.setProductId( data.get("productId") instanceof Double ? ((Double) data.get("productId")).longValue() : (Long) data.get("productId"));
        pi.setQuantity( data.get("quantity") instanceof Double ? ((Double) data.get("quantity")).intValue() : (Integer) data.get("quantity"));
        pi.setName((String) data.get("name"));
        pi.setImageUrl((String) data.get("imageUrl"));
        pi.setType((String) data.get("type"));
        pi.setAttributes((List<String>) data.get("attributes"));
        pi.setTotalUnitPrice(BigDecimal.valueOf((Double) data.get("totalUnitPrice")));

        return pi;
    }

    public static List<PurchaseItem> from (List<Map<String,Object>> items) {
        return items.stream().map(PurchaseItem::from).collect(Collectors.toList());
    }
}
