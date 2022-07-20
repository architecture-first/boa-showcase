package com.architecture.first.framework.business.retail.model.cashier.model.inventory;

import com.architecture.first.framework.business.retail.model.Discount;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Price {
    private BigDecimal value;
    private String type;
    private List<Discount> discounts;
}
