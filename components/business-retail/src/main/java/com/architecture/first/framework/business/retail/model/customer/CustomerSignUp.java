package com.architecture.first.framework.business.retail.model.customer;

import com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation.Address;
import com.architecture.first.framework.business.retail.model.cashier.model.order.payment.Payment;
import com.architecture.first.framework.security.model.SystemInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerSignUp extends SystemInfo {
    private String password;
    private String firstName;
    private String middleInitial;
    private String lastName;
    private Address billingAddress;
    private String emailAddress;
    private Long userId;
    private Boolean isRegistered;
    private Boolean isActive;
    private List<Payment> payment = new ArrayList<>();
}
