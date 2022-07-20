package com.architecture.first.framework.business.retail.model.cashier.model.order.cart;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class Order {
    private Long orderNumber;
    private Long userId;
    private List<PurchaseItem> purchaseItems;
    private BigDecimal shippingCost;
    private BigDecimal totalPrice;

    public static Order from(Map<String, Object> data) {    // handle marshalling due to Gson
        var order = new Order();
        order.setOrderNumber( data.get("orderNumber") instanceof Double ? ((Double) data.get("orderNumber")).longValue() : (Long) data.get("orderNumber"));
        order.setUserId( data.get("userId") instanceof Double ? ((Double) data.get("userId")).longValue() : (Long) data.get("userId"));
        order.setShippingCost(BigDecimal.valueOf((Double) data.get("shippingCost")));
        order.setTotalPrice(BigDecimal.valueOf((Double) data.get("totalPrice")));
        var purchaseItems = PurchaseItem.from((List<Map<String,Object>>) data.get("purchaseItems"));
        order.setPurchaseItems(purchaseItems);

        return order;
    }
}
