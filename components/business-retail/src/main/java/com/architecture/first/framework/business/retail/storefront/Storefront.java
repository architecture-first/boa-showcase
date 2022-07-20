package com.architecture.first.framework.business.retail.storefront;

import com.architecture.first.framework.business.retail.storefront.model.ICriteria;
import com.architecture.first.framework.business.retail.storefront.model.IProduct;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Repository
public class Storefront {
    @Autowired
    private JedisPooled jedis;

    public static String STORE_FRONT_NAME = "Storefront";
    public static String STORE_FRONT_CRITERIA_NAME = "Storefront/Criteria";

    public void addProducts(ICriteria criteria, List<? extends IProduct> products) {
        // get product Ids
        List<Long> productIds = new ArrayList<>();
        AtomicInteger productsAdded = new AtomicInteger(0);

        // store items
        products.forEach(p -> {
            productIds.add(p.getProductId());
            addProduct(p);
            productsAdded.incrementAndGet();
        });

        // update criteria
        jedis.hset(STORE_FRONT_CRITERIA_NAME, criteria.toString(), new Gson().toJson(productIds));
    }

    public void addProduct(IProduct p) {
        jedis.hset(STORE_FRONT_NAME, p.getProductId().toString(), new Gson().toJson(p));
    }

    public void removeProduct(IProduct p) {
        jedis.hdel(STORE_FRONT_NAME, p.getProductId().toString());
    }

    public Optional<IProduct> getProduct(Long productId, Type classType) {
        String contents = jedis.hget(STORE_FRONT_NAME, productId.toString());
        if (StringUtils.isEmpty(contents)) {
            return Optional.empty();
        }

        return Optional.of(new Gson().fromJson(contents, classType));
    }

    public Optional<List<IProduct>> getProducts(ICriteria criteria, Type classType) {
        String contents = jedis.hget(STORE_FRONT_CRITERIA_NAME, criteria.toString());
        if (StringUtils.isEmpty(contents)) {
            return Optional.empty();
        }

        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> productIds = new Gson().fromJson(contents, listType);
        List<IProduct> products = new ArrayList<>();
        productIds.forEach(p -> {
            var product = getProduct(Long.valueOf(p), classType);
            if (product.isPresent()) {
                products.add(product.get());
            }
        });

        // if the values do not equal then the cache is no longer accurate because a product was deleted
        if (products.size() != productIds.size()) {
            return Optional.empty();
        }

        return Optional.of(products);
    }
}
