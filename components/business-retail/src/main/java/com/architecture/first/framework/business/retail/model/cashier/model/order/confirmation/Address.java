package com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation;

import lombok.Data;

@Data
public class Address {
    private String street;
    private String city;
    private String state;
    private String zip;
}
