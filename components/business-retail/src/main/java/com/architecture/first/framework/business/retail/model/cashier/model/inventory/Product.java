package com.architecture.first.framework.business.retail.model.cashier.model.inventory;

import com.architecture.first.framework.business.retail.storefront.model.IProduct;
import lombok.Data;

import java.util.List;

@Data
public class Product implements IProduct {
    private Long productId;

    private String name;
    private String type;
    private String imageUrl;
    private List<String> attributes;
    private List<Price> price;
    private Integer unitsAvailable;
    private Boolean isActive;
//    private transient group updateDate;
    private String updatedBy;
}
