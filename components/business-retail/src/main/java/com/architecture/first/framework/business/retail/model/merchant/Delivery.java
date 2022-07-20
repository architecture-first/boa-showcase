package com.architecture.first.framework.business.retail.model.merchant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class Delivery {
    private Long productId;
    private Integer quantity;
}
