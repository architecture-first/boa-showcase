package com.architecture.first.merchant.actors;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.retail.storefront.Storefront;
import com.architecture.first.framework.security.model.UserToken;
import com.architecture.first.framework.technical.aop.RequestContext;
import com.architecture.first.merchant.repository.InventoryRepository;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

@Component
public class MerchantForTesting extends Merchant{
    public MerchantForTesting(InventoryRepository warehouse,
                              Storefront storefront) {
        super(warehouse, storefront);
    }

    @Override
    public String group() {return "Merchant";}

    @Override
    protected <T> Actor remember(String name, T value) {
        return super.remember(name, value);
    }

    @Override
    protected <T> Actor remember(String name, T value, Type classType) {
        return super.remember(name, value, classType);
    }

    // ex. recall("a", Integer.class);
    @Override
    protected <T> Optional<T> recall(String name, Class<T> classType) {
        return super.recall(name, classType);
    }

    // recall("int");
    @Override
    protected Optional<Object> recall(String name) {
        return super.recall(name);
    }

    @Override
    protected Map<String, Object> provideBrainDump() {
        return super.provideBrainDump();
    }

    @Override
    protected void requestNewToken(UserToken userToken) {super.requestNewToken(userToken);}
}
