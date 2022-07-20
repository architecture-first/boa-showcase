package com.architecture.first.framework.business.retail.model.results;

import com.architecture.first.framework.business.retail.model.Discount;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InventorySuggestedProductsResult {
    private Long productId;
    private String productName;
    private String imageUrl;
    private List<String> attributes;
    private BigDecimal originalPrice;
    private BigDecimal calculatedPrice;
    private List<Discount> discounts;
}
