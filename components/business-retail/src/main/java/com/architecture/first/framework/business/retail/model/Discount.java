package com.architecture.first.framework.business.retail.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class Discount {
    private String name;
    private String partOf;
    private BigDecimal value;
    private transient LocalDate effectiveDate;
    private transient LocalDate expirationDate;
}
