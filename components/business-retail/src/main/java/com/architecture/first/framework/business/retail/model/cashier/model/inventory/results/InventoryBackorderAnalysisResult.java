package com.architecture.first.framework.business.retail.model.cashier.model.inventory.results;

import lombok.Data;

@Data
public class InventoryBackorderAnalysisResult {
    private Long orderNumber;
    private Long productId;
    private Integer orderQuantity;      // how many items on order
    private Integer inventoryQuantity;  // how many items in inventory
    private Integer coveredQuantity;  // how many items covered by the available inventory
    private Integer backorderedQuantity; // how many items were back ordered
}
