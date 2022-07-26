package com.architecture.first.merchant.actors;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.retail.events.*;
import com.architecture.first.framework.business.retail.model.customer.cart.CartItem;
import com.architecture.first.framework.business.retail.model.merchant.Delivery;
import com.architecture.first.framework.business.retail.model.results.InventorySuggestedProductsResult;
import com.architecture.first.framework.business.retail.storefront.Storefront;
import com.architecture.first.framework.business.retail.storefront.model.IProduct;
import com.architecture.first.framework.business.vicinity.events.ActorNotFoundEvent;
import com.architecture.first.framework.business.vicinity.locking.Lock;
import com.architecture.first.framework.business.vicinity.tasklist.TaskTracking;
import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.business.actors.exceptions.ActorException;

import com.architecture.first.framework.technical.events.DefaultLocalEvent;
import com.architecture.first.framework.technical.util.SimpleModel;
import com.architecture.first.merchant.MerchantApplication;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.results.BonusPointAnalysisResult;
import com.architecture.first.merchant.repository.InventoryRepository;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.results.InventoryReorderAnalysisResult;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.Product;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class Merchant extends Actor {

    public static final int LIMIT_OF_PRODUCTS_FOR_CRITERIA = 100;
    private final InventoryRepository warehouse;
    private final Storefront storefront;

    @Value("${merchant.analysis.minimumAvailableProductThreshold}")
    private Integer minimumAvailableProductThreshold;

    @Value("${merchant.analysis.defaults.reorderQuantity}")
    private Integer defaultReorderQuantity;

    @Value("${merchant.analysis.numberOfOrdersToAnalyze}")
    private Integer numberOfOrdersToAnalyze;

    @Value("${merchant.analysis.numberOfProductsToSuggest}")
    private Integer numberOfProductsToSuggest;

    @Value("${merchant.analysis.bonusPoint.totalPriceThreshold}")
    private BigDecimal totalPriceThreshold;

    @Value("${merchant.analysis.bonusPoint.totalPriceFactor}")
    private BigDecimal totalPriceFactor;

    public static String TO_CUSTOMER = "Customer";

    @Autowired
    public Merchant(InventoryRepository warehouse,
                    Storefront storefront) {
        this.warehouse = warehouse;
        this.storefront = storefront;

        setGeneration("1.0.2");
    }

    @Override
    protected void init() {
        super.init();
        registerBehavior("SupplyProductsHaveArrived", Merchant.hearSupplyProductsHaveArrived);
        registerBehavior("RemoveReservations", Merchant.hearRemoveReservationRequest);
        registerBehavior("RequestBonusPoints", Merchant.hearBonusPointsRequest);
        registerBehavior("ViewProducts", Merchant.hearViewProductsRequest);
        registerBehavior("ViewProduct", Merchant.hearViewProductRequest);
        registerBehavior("CheckoutRequest", Merchant.hearCheckoutRequest);
        registerBehavior("OrderSupplyProducts", Merchant.hearOrderSuppliesRequest);
        registerBehavior("CrossSellsUpdated", Merchant.hearCrossSellsUpdated);

    }

    // M01-Show-Products
    public List<? extends IProduct> showProducts(ArchitectureFirstEvent localEvent) {
        return showProducts(new ViewProductsEvent(this, "Merchant", "self")
                .initFromDefaultEvent(localEvent));
    }

    @TaskTracking(task = "merchant/ShowProducts", defaultParentTask = "customer/ViewProducts")
    public List<? extends IProduct> showProducts(ViewProductsEvent event) {
        // Try to get products from storefront.  If they are not there then get from warehouse
        var optionalProducts = storefront.getProducts(event.getCriteria(), Product.class);
        var products =  (optionalProducts.isPresent())
                ? optionalProducts.get()
                : warehouse.getProducts(event.getCriteria());

        /* At step 1, the merchant cannot find available products.
            2.1 The merchant notifies the customer.*/
        // Note: This will also notify other interested customers in the vicinity
        if (products.size() == 0) {
            say(new NoProductsAvailableEvent(this, this.name(), event.from())
                    .setRequestId(event.getRequestId())
                    .setAccessToken(event.getAccessToken()));
            rememberOccurrence("No Products found for criteria", event.getCriteria().getJsonCriteria());
        }
        else {
            if (optionalProducts.isEmpty()) {
                storefront.addProducts(event.getCriteria(), products);
            }
        }

        return products;
    }

    // M08-Show-ProductF
    @TaskTracking(task = "merchant/ShowProduct", defaultParentTask = "customer/ViewProduct")
    public CartItem showProduct(ViewProductEvent event) {
        var optionalProduct = storefront.getProduct(event.getProductId(), Product.class);
        var product = (optionalProduct.isPresent())
                                ? (Product) optionalProduct.get()
                                : warehouse.getProductById(event.getProductId());

        Merchant ths = (Merchant) AopContext.currentProxy();
        ths.suggestProductsForCustomer(event);

        return CartItem.from(product);
    }

    // M04-Review Inventory
    @TaskTracking(task = "merchant/ReviewInventory")
    public Boolean reviewInventory(ReviewInventoryEvent event) {
        List<InventoryReorderAnalysisResult> candidatesForReordering = findCandidatesForReordering(minimumAvailableProductThreshold);
        Merchant ths = (Merchant) AopContext.currentProxy();

        candidatesForReordering.stream().forEach(result -> {
            log.info("Reorder product " + result.getProductId());
            Long productId = result.getProductId();
            Integer orderQuantity = result.getOrderQuantity();

            // TODO - Add additional logic to determine if something should be reordered

            // M05 - Order Supplies / Record Units on Order
            var orderSupplyProductsEvent = new OrderSupplyProductsEvent(this, this.name(), this.name())
                    .addProduct(productId, orderQuantity)
                    .setOriginalEvent(event);
            whisper(orderSupplyProductsEvent);
            ths.recordUnitsOnOrder(orderSupplyProductsEvent, productId, orderQuantity);
        });

        return true;
    }

    protected List<InventoryReorderAnalysisResult> findCandidatesForReordering(Integer minimumAvailableQuantity) {
        return warehouse.findItemsToPotentiallyReorder(minimumAvailableQuantity);
    }

    // M03 - Reserve on Back Order
    @TaskTracking(task = "merchant/ReserveOnBackOrder", defaultParentTask = "merchant/ReserveItems")
    public long reserveOnBackOrder(CheckoutRequestEvent event, Long productId, Integer unitsBackordered) {
        return warehouse.reserveOnBackOrder(productId, unitsBackordered);
    }

    // M05 - Order Supplies / Record Units
    @TaskTracking(task = "merchant/RecordUnits", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    public long recordUnitsOnOrder(ArchitectureFirstEvent event, Long productId, Integer unitsOnOrder) {
        return warehouse.recordUnitsOnOrder(productId, unitsOnOrder);
    }

    // M05 - Order Supplies / Record Deliveries
    @TaskTracking(task = "merchant/RecordDeliveries", defaultParentTask = "merchant/OrderSupplies")
    protected long recordSupplyOrderHistory(SupplyProductsHaveArrivedEvent event, Long productId, Integer quantity) {
        return warehouse.recordSupplyOrderHistory(productId, quantity);
    }

    // M06 - Suggest Products
    @TaskTracking(task = "merchant/SuggestProducts")
    protected void suggestProductsForCustomer(ViewProductEvent event) {
        Long customerId = (event.hasAccessToken())
                ? SecurityGuard.getUserId(event.getAccessToken())
                : 0;
        String suggestedProductsKey = "SuggestedProductsFor_" + customerId + "_" + event.getProductId();
        Class<? extends ArrayList> clss = new ArrayList<InventorySuggestedProductsResult>().getClass();

        // Try to recall suggested products. If data is not there then get the products and then remember.
        var suggestedProducts = recall(suggestedProductsKey, clss).isPresent() ?
                recall(suggestedProductsKey, clss).get()
                : findSuggestedProductsForCustomer(event, customerId, event.getProductId());

        if (exists(suggestedProducts)) {
            // Remember the suggested products for a while (also saves a backend call)
            remember(suggestedProductsKey, suggestedProducts, clss);

            // Inform the customer or others listening
            say(new SuggestedProductsEvent(this, name(), event.from())
                    .addProducts(suggestedProducts)
                    .setRequestId(event.getRequestId())
                    .setAccessToken(event.getAccessToken())
                    .setOriginalEvent(event)
            );
        }
    }

    // M07 - Remove Reservation
    @TaskTracking(task = "merchant/RecordDeliveries", defaultParentTask = "merchant/OrderSupplies")
    protected long removeReservation(RemoveReservationsEvent event, Long productId, Integer unitsBackordered) {
        return warehouse.removeReservation(productId, unitsBackordered);
    }

    protected List<InventorySuggestedProductsResult> findSuggestedProductsForCustomer(ViewProductEvent event, Long customerId, Long productId) {
        var suggestedProducts = warehouse.getSuggestedProducts(customerId, productId, numberOfOrdersToAnalyze, numberOfProductsToSuggest);

        return suggestedProducts.stream().map(s -> {
            var p = warehouse.getProductById(s.getProductId());
            if (p != null) {
                s.setProductName(p.getName());
                s.setImageUrl(p.getImageUrl());
            }
            return s;
        }).collect(Collectors.toList());
    }

    // M09 - Give Bonus Points
    @TaskTracking(task = "merchant/GiveBonusPoints", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected long addBonusPointsToOrder(RequestBonusPointsEvent event, Long orderNumber, BigDecimal bonusPointsEarned) {
        return warehouse.addBonusPointsToOrder(orderNumber, bonusPointsEarned);
    }

    // M02 - Reserve Items
    @TaskTracking(task = "merchant/ReserveItems", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected long reserveItems(CheckoutRequestEvent event, Long orderNumber, List<CartItem> items) {
         final Merchant ths = (Merchant) AopContext.currentProxy();

        items.forEach(item -> {
            if (ths.recordUnitsOnOrder(event, item.getProductId(), item.getQuantity()) != 1) {
                if (ths.reserveOnBackOrder(event, item.getProductId(), item.getQuantity()) != 1) {
                    // store on bulletin board if warehouse fails and store in warehouse (a.k.a. database later)
                    var key = String.format("%s/%s", orderNumber, item.getProductId());
                    bulletinBoard().postTopic("ItemsToReserveOnBackorder", key, item.getQuantity().toString());
                }
            }
        });

        return 1;
    }

    // M09 - Give Bonus Points / Analysis
    @TaskTracking(task = "merchant/DetermineBonusPoints", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected List<BonusPointAnalysisResult> determineBonusPoints(RequestBonusPointsEvent bonusPointsEvent, BigDecimal totalPriceThreshold, BigDecimal totalPriceFactor) {
        var results = warehouse.determineBonusPoints(bonusPointsEvent.getCustomerId(), totalPriceThreshold, totalPriceFactor);
        results.forEach(r -> {
            EarnedBonusPointsEvent event = new EarnedBonusPointsEvent(this, name(), TO_CUSTOMER);
            event.payload().putAll(convertToMap(r));
        });

        return results;
    }

    // M03 - Reserve On Back Order
    @TaskTracking(task = "merchant/OrderSupplyProducts", defaultParentTask = "merchant/ReviewInventory")
    protected boolean orderSupplyProducts(OrderSupplyProductsEvent event) {
        //...
        return true;
    }

    public ArchitectureFirstEvent onExternalBehavior(ArchitectureFirstEvent event) {
        if (event instanceof AcquireCrossSellProductsEvent) {
            var results =  behavior().perform(event);
            if (results.isPresent()) {
                log.info("Cross Sells: " + results.get());
                var evt = (AcquireCrossSellProductsEvent) event;
                evt.addCrossSells("data", results.get());
                return evt;
            }
        }

        return event;
    }

    @TaskTracking(task = "merchant/AcquireCrossSellProducts")
    public SimpleModel showCrossSells(DefaultLocalEvent localEvent) {
        var results = new SimpleModel();
        this.whisper(new AcquireCrossSellProductsEvent(this, name(), name())
                        .initFromDefaultEvent(localEvent),
                        r -> {
                            if (r instanceof AcquireCrossSellProductsEvent) {
                                var evt = (AcquireCrossSellProductsEvent) r;
                                results.put("results", evt.getCrossSells());
                                return true;
                            }
                            return false;
                        });

        return results;
    }

    private void processDeliveries() {
        var signature = "Queue/Merchant/Deliveries";
        Gson gson = new Gson();

        Delivery delivery;
        do {
            delivery = queue().pop(signature, Delivery.class);
            if (delivery != null) {
                warehouse.recordSupplyOrderHistory(delivery.getProductId(), delivery.getQuantity());
            }
        }
        while (delivery != null);

    }

    protected static Function<ArchitectureFirstEvent, Actor> hearSupplyProductsHaveArrived = (event -> {
        final Merchant ths = (Merchant) AopContext.currentProxy();
        SupplyProductsHaveArrivedEvent evt = (SupplyProductsHaveArrivedEvent) event;
        evt.getProductsThatArrived().forEach((productId, quantity) -> {
            evt.getTarget().ifPresentOrElse(
                    (m) -> ths.recordSupplyOrderHistory(evt, productId, quantity),
                    () -> {throw new ActorException(evt.getTarget().get(), "id: " + productId + " not found");}
            );
        });

        // Handle any additional deliveries
        var actor = (Merchant) event.getTarget().get();
        actor.processDeliveries();

        return actor;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearRemoveReservationRequest = (event -> {
        final Merchant ths = (Merchant) AopContext.currentProxy();
        RemoveReservationsEvent evt = (RemoveReservationsEvent) event;

        evt.getProductReservationsToRemove().forEach((productId, quantity) -> {
            evt.getTarget().ifPresentOrElse(
                    (m) -> ths.removeReservation(evt, productId, quantity),
                    () -> {throw new ActorException(ths, "id: " + productId + " not found");}
            );
        });

        return event.getTarget().get();
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearBonusPointsRequest = (event -> {
        RequestBonusPointsEvent evt = (RequestBonusPointsEvent) event;
        final Merchant ths = (Merchant) AopContext.currentProxy();

        var list = ths.determineBonusPoints(evt,
                ths.totalPriceThreshold, ths.totalPriceFactor);
        list.forEach(r -> {
            ths.addBonusPointsToOrder(evt, r.getOrderNumber(), r.getBonusPoints());
            ths.say(new EarnedBonusPointsEvent(ths, ths.name(), TO_CUSTOMER)
                    .setCustomerId(r.getCustomerId()).setOrderNumber(r.getOrderNumber()));
        });

        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearOrderSuppliesRequest = (event -> {
        var evt = (OrderSupplyProductsEvent) event;
        final Merchant ths = (Merchant) AopContext.currentProxy();

        ths.orderSupplyProducts(evt);

        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearViewProductsRequest = (event -> {
        var evt = (ViewProductsEvent) event;
        final Merchant ths = (Merchant) AopContext.currentProxy();

        evt.addProducts((List<Product>) ths.showProducts(evt));
        evt.reply(ths.name());
        ths.say(evt);

        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearViewProductRequest = (event -> {
        var evt = (ViewProductEvent) event;
        final Merchant ths = (Merchant) AopContext.currentProxy();

        evt.setProduct(ths.showProduct(evt));
        evt.reply(ths.name());
        ths.say(evt);

        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearCheckoutRequest = (event -> {
        var evt = (CheckoutRequestEvent) event;
        final Merchant ths = (Merchant) AopContext.currentProxy();

        var requestPaymentEvent = ths.reserveItems(evt, evt.getOrderNumber(), evt.getShoppingCart().getItems());

        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearCrossSellsUpdated = (event -> {
        var evt = (CrossSellsUpdatedEvent) event;
        final Merchant ths = (Merchant) AopContext.currentProxy();

        evt.reply(ths.name());
        ths.say(evt);

        return ths;
    });
    @Override
    protected void onThink() {
        try {
            super.onThink();

            // Note: Add custom internal processing
        }
        catch (Exception e) {
            log.error("Error with internal processing", e);
        }
    }

    /**
     * Perform processing on 30 min into an hour
     */
    @Override
    protected void on30min() {
        processDeliveries();

        if (isMyTurn()) {
            try {
                if (!lock().attemptLock("ReviewInventory", name())
                        .equals(Lock.FAILED_LOCK_ATTEMPT)) {

                    Merchant ths = (Merchant) AopContext.currentProxy();
                    ths.reviewInventory(new ReviewInventoryEvent(this, name(), name()));
                }
            } finally {
                lock().unlock("ReviewInventory", name());
            }
        }
    }

    @Override
    protected void on24hours() {
        var advertiser = vicinity().findActor("Advertiser");
        if (StringUtils.isNotEmpty(advertiser)) {
            say(new AcquireCrossSellProductsEvent(this, name(), advertiser));
        }
        else {
            log.warn("Advertiser not found");
            announce(new ActorNotFoundEvent(this, name(), SecurityGuard.VICINITY_MONITOR));
        }
    }

    @Override
    protected void onUnhandledEvent(ArchitectureFirstEvent event) {
        // .. override to handle
        log.warn("unhandled event: " + event.toString(), event);
    }

    @Override
    protected void onTerminate(String reason) {
        super.onTerminate(reason);
        log.info("Terminating Merchant: " + name());

        MerchantApplication.close();
    }
}
