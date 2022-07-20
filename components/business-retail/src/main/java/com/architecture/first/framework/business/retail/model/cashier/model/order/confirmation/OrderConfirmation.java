package com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation;

import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.PurchaseItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class OrderConfirmation {
    private Address billingAddress;
    private Integer bonusPointsEarned;
    private Long customerId;
    private Date datePurchased;
    private String emailAddress;
    private List<OrderConfirmationItem> items;
    private List<PurchaseItem> purchaseItems;
    private Long orderNumber;
    private Address shippingAddress;
    private BigDecimal shippingCost;
    private BigDecimal totalPrice;
}
