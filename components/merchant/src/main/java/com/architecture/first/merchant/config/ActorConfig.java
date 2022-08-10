package com.architecture.first.merchant.config;

import com.architecture.first.framework.business.retail.storefront.Storefront;
import com.architecture.first.merchant.actors.Merchant;
import com.architecture.first.merchant.repository.InventoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to support Actors
 */
@Configuration
public class ActorConfig {

    @Bean
    public Merchant merchant(InventoryRepository warehouse, Storefront storefront) {
        return new Merchant(warehouse, storefront);
    }
}
