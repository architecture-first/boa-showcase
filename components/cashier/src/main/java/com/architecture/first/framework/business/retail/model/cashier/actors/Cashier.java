package com.architecture.first.framework.business.retail.model.cashier.actors;

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
public class Cashier extends Actor {

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
    protected RequestPaymentEvent requestPayment(CheckoutRequestEvent checkoutRequestEvent) {
        var order = checkout.getOrder(checkoutRequestEvent.getCustomerId(), checkoutRequestEvent.getOrderNumber(),"cart");

        var purchaseItems = sumPurchaseItems(order.getPurchaseItems());
        order.setPurchaseItems(purchaseItems);
        var totalUnitPrice = calculateTotalUnitPrice(purchaseItems);
        order.setShippingCost(calculateShippingCost(totalUnitPrice));
        order.setTotalPrice(totalUnitPrice.add(order.getShippingCost()));
        checkout.updateOrderTotals(checkoutRequestEvent.getCustomerId(), checkoutRequestEvent.getOrderNumber(),
                order.getTotalPrice(), order.getShippingCost());

        checkoutRequestEvent.setOrderPreview(order);
        Cashier ths = (Cashier) AopContext.currentProxy();

        var event = new RequestPaymentEvent(this, name(), checkoutRequestEvent.from())
                .setCustomerId(checkoutRequestEvent.getCustomerId())
                .setOrderPreview(order)
                .setOriginalEvent(checkoutRequestEvent);

        say(event);

        return (RequestPaymentEvent) event;
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

    private Order prepareAndProcessOrder(Cashier ths, PaymentResponseEvent evt) {
        var cashier = (Cashier) evt.getTarget().get();
        var order = cashier.checkout.getOrder(evt.getCustomerId(), evt.getOrderNumber(),"cart");
        ths.processPayment(evt, evt.getCustomerId(), order);
        return order;
    }

    // S02 - Process Payment
    @TaskTracking(task = "cashier/ProcessPayment", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void processPayment(PaymentResponseEvent event, Long customerId, Order order) {
        var paymentInfo = checkout.getPaymentInformation(customerId);

        Cashier ths = (Cashier) AopContext.currentProxy();
        if (ths.canCompleteOrderWithBankingSystem(event, new PaymentInfo(), order.getTotalPrice())) {
            ths.storeOrderHistory(event, customerId, order);
            ths.presentOrderConfirmation(event);
            return;
        }

        ths.notifyCustomerOfPaymentFailure(event);
    }

    // S03 - Present Order Confirmation
    @TaskTracking(task = "cashier/PresentOrderConfirmation", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void presentOrderConfirmation(PaymentResponseEvent paymentEvent) {
        var replyEvent = OrderConfirmationEvent.fromForReply(name(), this, paymentEvent);
        replyEvent.setCustomerId(paymentEvent.getCustomerId());

        var orderConfirmation = checkout.getOrderConfirmation(replyEvent.getCustomerId(), paymentEvent.getOrderNumber());
        var summedOrderItems = orderConfirmation.getItems().stream()
                .map(i -> PurchaseItem.from(i)).collect(Collectors.toList());
        orderConfirmation.setPurchaseItems(sumPurchaseItems(summedOrderItems));
        orderConfirmation.setItems(new ArrayList<>());  // remove unnecessary payload

        replyEvent.setOrderConfirmation(orderConfirmation);

        say(replyEvent);
    }

    // S04 - Store Order History
    @TaskTracking(task = "cashier/StoreOrderHistory", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void storeOrderHistory(PaymentResponseEvent event, Long customerId, Order order) {
        Cashier ths = (Cashier) AopContext.currentProxy();
        ths.confirmOrder(event, customerId, order);
    }

    @TaskTracking(task = "cashier/CompleteOrderWithBankingSystem", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected boolean canCompleteOrderWithBankingSystem(PaymentResponseEvent event, PaymentInfo paymentInfo, BigDecimal totalPrice) {
        // ...
        return true;
    }

    @TaskTracking(task = "cashier/ConfirmOrder", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void confirmOrder(PaymentResponseEvent event, Long customerId, Order order) {
        checkout.updateOrderStatus(customerId, order.getOrderNumber(), "cart", "processed");
    }

    @TaskTracking(task = "cashier/NotifyCustomerOfPaymentFailure", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void notifyCustomerOfPaymentFailure(PaymentResponseEvent paymentEvent) {
        var replyEvent = PaymentFailureEvent.fromForReply(name(), this, paymentEvent);
        say(replyEvent);
    }

    protected static Function<ArchitectureFirstEvent, Actor> hearCheckoutRequest = (event -> {
        var evt = (CheckoutRequestEvent) event;
        final Cashier ths = (Cashier) AopContext.currentProxy();

        ths.requestPayment(evt);

        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearPaymentResponse = (event -> {
        var evt = (PaymentResponseEvent) event;
        final Cashier ths = (Cashier) AopContext.currentProxy();

        log.info("Payment response received");
        if (evt.getApprovalStatus() == true) {
            log.info("customer approved this payment");
            Order order = ths.prepareAndProcessOrder(ths, evt);
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
