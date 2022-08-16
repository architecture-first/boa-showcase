package com.architecture.first.framework.business.retail.model.cashier.actors;

import com.architecture.first.framework.business.BusinessActor;
import com.architecture.first.framework.business.retail.events.*;
import com.architecture.first.framework.business.retail.model.cashier.CashierApplication;
import com.architecture.first.framework.business.retail.model.cashier.model.PaymentInfo;
import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.Order;
import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.PurchaseItem;
import com.architecture.first.framework.business.retail.model.cashier.repository.OrderRepository;
import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.vicinity.tasklist.TaskTracking;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Cashier extends BusinessActor {

    @Autowired
    private OrderRepository checkout;

    public Cashier() {
        setGeneration("1.0.2");
    }

    @Override
    protected void init() {
        super.init();

        registerBehavior("CheckoutRequest", Cashier.hearCheckoutRequest);
        registerBehavior("PaymentResponse", Cashier.hearPaymentResponse);
    }

    // S01 - Request Payment
    @TaskTracking(task = "cashier/RequestPayment", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected ArchitectureFirstEvent requestPayment(ArchitectureFirstEvent checkoutRequestEvent) {
        var userId = (Long) checkoutRequestEvent.getPayloadValueAs("userId", Long.class);
        var orderNumber = (Long) checkoutRequestEvent.getPayloadValueAs("orderNumber", Long.class);

        var order = checkout.getOrder(userId, orderNumber,"cart");

        var purchaseItems = sumPurchaseItems(order.getPurchaseItems());
        order.setPurchaseItems(purchaseItems);
        var totalUnitPrice = calculateTotalUnitPrice(purchaseItems);
        order.setShippingCost(calculateShippingCost(totalUnitPrice));
        order.setTotalPrice(totalUnitPrice.add(order.getShippingCost()));
        checkout.updateOrderTotals(userId, orderNumber,
                order.getTotalPrice(), order.getShippingCost());

        checkoutRequestEvent.setPayloadValue("orderPreview", order);
        Cashier ths = (Cashier) AopContext.currentProxy();

        var event = new ArchitectureFirstEvent(this, "RequestPaymentEvent", name(), checkoutRequestEvent.from())
                .setPayloadValue("userId", userId)
                .setPayloadValue("orderNumber", order.getOrderNumber())
                .setPayloadValue("orderPreview", order)
                .setOriginalEvent(checkoutRequestEvent);

        say(event);

        return event;
    }

    private List<PurchaseItem> sumPurchaseItems(List<PurchaseItem> purchaseItems) {
        // sum up quantities of like items
        var cartItems = purchaseItems.stream().collect(Collectors.groupingBy((p)->p.getProductId())) //expression
                .values()
                .stream()
                .flatMap(e->e.stream().limit(1))
                .collect(Collectors.toList());

        var quantities = purchaseItems.stream().collect(Collectors.groupingBy(PurchaseItem::getProductId,
                Collectors.summingInt(PurchaseItem::getQuantity)));

        cartItems.forEach(c -> {
            c.setQuantity(quantities.get(c.getProductId()));
            var p = checkout.getProductById(c.getProductId());
            c.setName(p.getName());
            c.setType(p.getType());
            c.setImageUrl(p.getImageUrl());
            c.setAttributes(p.getAttributes());
        });

        return cartItems;
    }

    private BigDecimal calculateTotalUnitPrice(List<PurchaseItem> purchaseItems) {
        return purchaseItems.stream().map(i -> i.getTotalUnitPrice().multiply(new BigDecimal(i.getQuantity().toString())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateShippingCost(BigDecimal totalUnitCost) {
        return totalUnitCost.multiply(new BigDecimal("0.1"))
                .round(new MathContext(2, RoundingMode.HALF_UP));
    }

    private Order prepareAndProcessOrder(Cashier ths, ArchitectureFirstEvent evt) {
        var userId = (Long) evt.getPayloadValueAs("userId", Long.class);
        var orderNumber = (Long) evt.getPayloadValueAs("orderNumber", Long.class);

        var cashier = (Cashier) evt.getTarget().get();
        var order = cashier.checkout.getOrder(userId, orderNumber,"cart");
        ths.processPayment(evt, userId, order);
        return order;
    }

    // S02 - Process Payment
    @TaskTracking(task = "cashier/ProcessPayment", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void processPayment(ArchitectureFirstEvent event, Long userId, Order order) {
        var paymentInfo = checkout.getPaymentInformation(userId);

        Cashier ths = (Cashier) AopContext.currentProxy();
        if (ths.canCompleteOrderWithBankingSystem(event, new PaymentInfo(), order.getTotalPrice())) {
            ths.storeOrderHistory(event, userId, order);
            ths.presentOrderConfirmation(event);
            return;
        }

        ths.notifyCustomerOfPaymentFailure(event);
    }

    // S03 - Present Order Confirmation
    @TaskTracking(task = "cashier/PresentOrderConfirmation", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void presentOrderConfirmation(ArchitectureFirstEvent paymentEvent) {
        var userId = (Long) paymentEvent.getPayloadValueAs("userId", Long.class);
        var orderNumber = (Long) paymentEvent.getPayloadValueAs("orderNumber", Long.class);

        var replyEvent = ArchitectureFirstEvent.fromForReply(this, "OrderConfirmationEvent", ArchitectureFirstEvent.EVENT_TYPE_SECURED_BASIC,
                name(), paymentEvent, true);
        replyEvent.setPayloadValue("userId",userId);

        var orderConfirmation = checkout.getOrderConfirmation(userId, orderNumber);
        var summedOrderItems = orderConfirmation.getItems().stream()
                .map(i -> PurchaseItem.from(i)).collect(Collectors.toList());
        orderConfirmation.setPurchaseItems(sumPurchaseItems(summedOrderItems));
        orderConfirmation.setItems(new ArrayList<>());  // remove unnecessary payload

        replyEvent.setPayloadValue("orderConfirmation", orderConfirmation);

        say(replyEvent);
    }

    // S04 - Store Order History
    @TaskTracking(task = "cashier/StoreOrderHistory", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void storeOrderHistory(ArchitectureFirstEvent event, Long userId, Order order) {
        Cashier ths = (Cashier) AopContext.currentProxy();
        ths.confirmOrder(event, userId, order);
    }

    @TaskTracking(task = "cashier/CompleteOrderWithBankingSystem", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected boolean canCompleteOrderWithBankingSystem(ArchitectureFirstEvent event, PaymentInfo paymentInfo, BigDecimal totalPrice) {
        // ...
        return true;
    }

    @TaskTracking(task = "cashier/ConfirmOrder", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void confirmOrder(ArchitectureFirstEvent event, Long userId, Order order) {
        checkout.updateOrderStatus(userId, order.getOrderNumber(), "cart", "processed");
    }

    @TaskTracking(task = "cashier/NotifyCustomerOfPaymentFailure", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void notifyCustomerOfPaymentFailure(ArchitectureFirstEvent paymentEvent) {
        var replyEvent = ArchitectureFirstEvent.fromForReply(this, name(), paymentEvent);
        say(replyEvent);
    }

    protected static Function<ArchitectureFirstEvent, Actor> hearCheckoutRequest = (event -> {
        final Cashier ths = (Cashier) AopContext.currentProxy();

        ths.requestPayment(event);

        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearPaymentResponse = (event -> {
        final Cashier ths = (Cashier) AopContext.currentProxy();

        log.info("Payment response received");
        if ((Boolean) event.getPayloadValue("approvalStatus") == true) {
            log.info("customer approved this payment");
            Order order = ths.prepareAndProcessOrder(ths, event);
        }

        return ths;
    });

    @Override
    protected void onTerminate(String reason) {
        super.onTerminate(reason);
        log.info("Terminating Cashier: " + name());

        CashierApplication.stop();
    }
}
