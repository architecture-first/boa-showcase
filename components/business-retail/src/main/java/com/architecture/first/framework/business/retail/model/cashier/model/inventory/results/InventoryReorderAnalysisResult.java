package com.architecture.first.framework.business.retail.model.cashier.model.inventory.results;

import lombok.Data;

@Data
public class InventoryReorderAnalysisResult {
    private Long productId;
    private Integer orderQuantity;      // how many items on order
    private Integer inventoryQuantity;  // how many items in inventory
    private Integer availableQuantity;  // inventoryQuantity - orderQuantity
}
