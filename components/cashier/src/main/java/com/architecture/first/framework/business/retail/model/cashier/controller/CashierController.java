package com.architecture.first.framework.business.retail.model.cashier.controller;

import com.architecture.first.framework.business.retail.events.CheckoutRequestEvent;
import com.architecture.first.framework.business.retail.model.cashier.actors.Cashier;
import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.Order;
import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.technical.user.UserInfo;
import com.architecture.first.framework.technical.util.SimpleModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@RestController
@RequestMapping("api/cashier")
public class CashierController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private Cashier cashier;

    @Autowired
    private ApplicationEventPublisher publisher;

    @PostMapping("checkout")
    public Order checkout() {
        var event = new CheckoutRequestEvent("Source", "CashierController", Arrays.asList("Cashier"));
        event.setCustomerId(SecurityGuard.getUserId(((UserInfo) request.getAttribute("userInfo")).getAccessToken()));
        event.setRequestId((String) request.getAttribute("requestId"));
        event.awaitResponse();

        final SimpleModel orderHolder = new SimpleModel();

        cashier.whisper(event, e -> {
           var evt = (CheckoutRequestEvent) e;
           orderHolder.put("order", evt.getOrderPreview());
           return true;
        });

        return (Order) orderHolder.get("order");
    }

}
