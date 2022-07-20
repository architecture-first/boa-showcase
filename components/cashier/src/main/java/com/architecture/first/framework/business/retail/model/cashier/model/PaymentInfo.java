package com.architecture.first.framework.business.retail.model.cashier.model;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class PaymentInfo {
    private String type;
    private String name;
    private String number;
    private ZonedDateTime expirationDate;
    private Boolean isDefault;
}
