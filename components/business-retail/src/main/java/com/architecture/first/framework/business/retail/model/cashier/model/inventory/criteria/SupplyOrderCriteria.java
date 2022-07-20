package com.architecture.first.framework.business.retail.model.cashier.model.inventory.criteria;

import com.architecture.first.framework.business.retail.storefront.model.ICriteria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class SupplyOrderCriteria implements ICriteria {
    private Long productId;
    private Integer unitsOnOrder;
}
