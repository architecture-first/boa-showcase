package com.architecture.first.framework.business.retail.model.cashier.model.inventory.results;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BonusPointAnalysisResult {
    private Long orderNumber;
    private Long customerId;
    private BigDecimal bonusPoints;
    private BigDecimal totalPrice;
}
