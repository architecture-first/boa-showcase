package com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation;

import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.PurchaseItem;
import com.architecture.first.framework.business.retail.model.customer.cart.CartItem;
import lombok.Data;

@Data
public class OrderConfirmationItem extends CartItem {

    public OrderConfirmationItem() {super();}

    public OrderConfirmationItem(PurchaseItem purchaseItem) {
        super(purchaseItem);
    }
}
