package com.architecture.first.framework.business.retail.model.vendor.actors;

import com.architecture.first.framework.business.retail.events.*;
import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.retail.model.merchant.Delivery;
import com.architecture.first.framework.business.retail.model.vendor.VendorApplication;
import com.architecture.first.framework.business.vicinity.queue.Queue;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class Vendor extends Actor {
    @Autowired
    private Queue queue;

    private final Gson gson = new Gson();

    public Vendor() {
        setGeneration("1.0.2");
    }

    public Vendor() {
        setVersion("1.0.1");
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void on24hours() {
        var merchant = vicinity().findActor("Merchant");
        if (StringUtils.isNotEmpty(merchant)) { // send a "nice to have" event, but Merchants will look for data anyway
            say(new SupplyProductsHaveArrivedEvent(this, name(), merchant)
                    .addProduct(1001l, 10));
        }

        addAdditionalDeliveriesToQueue();
    }

    private void addAdditionalDeliveriesToQueue() {
        var signature = "Queue/Merchant/Deliveries";
        queue.create(signature);

        Map<Long,Integer> deliveries = Map.of(1001l, 10, 1002l, 20);

        deliveries.entrySet().forEach(e -> {
            var delivery = new Delivery(e.getKey(), e.getValue());
            queue.push(signature, delivery, Delivery.class);
        });

    }

    @Override
    protected void onTerminate(String reason) {
        super.onTerminate(reason);
        log.info("Terminating Cashier: " + name());

        VendorApplication.stop();
    }
}
