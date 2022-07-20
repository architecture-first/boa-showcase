package com.architecture.first.framework.business.retail.model.customer;

import com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation.Address;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerRegistration {
    private Address billingAddress;
    private BigDecimal bonusPoints;
    private Long userId;
    private String emailAddress;
    private String firstName;
    private String middleInitial;
    private String lastName;
    private Integer unitsOnOrder;
    private String updatedBy;
}
