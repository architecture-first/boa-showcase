package com.architecture.first.framework.business.retail.model.vendor.actors;

import com.architecture.first.framework.business.BusinessActor;
import com.architecture.first.framework.business.retail.events.*;
import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.retail.model.merchant.Delivery;
import com.architecture.first.framework.business.retail.model.vendor.VendorApplication;
import com.architecture.first.framework.business.vicinity.queue.Queue;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class Vendor extends BusinessActor {
    @Autowired
    private Queue queue;

    private final Gson gson = new Gson();

    public Vendor() {
        setGeneration("1.0.2");
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void on24hours() {
        var merchant = vicinity().findActor("Merchant");
        if (StringUtils.isNotEmpty(merchant)) { // send a "nice to have" event, but Merchants will look for data anyway
            Map<Long,Integer> productsThatArrived =  new HashMap<>();
            productsThatArrived.put(1001l, 10);

            say(new ArchitectureFirstEvent(this, "SupplyProductsHaveArrivedEvent", name(), merchant)
                    .setPayloadValue("productsThatArrived", productsThatArrived);
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
