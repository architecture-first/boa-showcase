package com.architecture.first.framework.business.retail.model.customer.cart;

import com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation.Address;
import com.architecture.first.framework.business.retail.model.customer.CustomerRegistration;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class ShoppingCart {
    private Long orderNumber;
    private Address billingAddress;
    private Long userId;
    private String emailAddress;
    private String firstName;
    private String lastName;
    private String middleInitial;
    private Boolean isActive;
    private List<CartItem> items = new ArrayList<>();
    private String status = "cart";
    private Date updateDate;
    private String updatedBy;

    public ShoppingCart init(CustomerRegistration registration) {
        this.billingAddress = registration.getBillingAddress();
        this.userId = registration.getUserId();
        this.emailAddress = registration.getEmailAddress();
        this.firstName = registration.getFirstName();
        this.middleInitial = registration.getMiddleInitial();
        this.lastName = registration.getLastName();

        return this;
    }

    public ShoppingCart addItem(CartItem item) {
        this.items.add(item);
        return this;
    }

    public ShoppingCart addItems(List<CartItem> items) {
        this.items.addAll(items);
        return this;
    }
}
