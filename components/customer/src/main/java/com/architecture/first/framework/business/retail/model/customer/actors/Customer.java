package com.architecture.first.framework.business.retail.model.customer.actors;

import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import com.architecture.first.framework.business.BusinessActor;
import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.actors.exceptions.ActorException;
import com.architecture.first.framework.business.hub.ClientCommunication;
import com.architecture.first.framework.business.retail.events.*;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.Product;
import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.Order;
import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.PurchaseItem;
import com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation.OrderConfirmation;
import com.architecture.first.framework.business.retail.model.criteria.ShowProductsCriteria;
import com.architecture.first.framework.business.retail.model.customer.CustomerSignUp;
import com.architecture.first.framework.business.retail.model.customer.cart.CartItem;
import com.architecture.first.framework.business.retail.model.customer.cart.ShoppingCart;
import com.architecture.first.framework.business.retail.model.customer.repository.CustomerRepository;
import com.architecture.first.framework.business.retail.model.results.InventorySuggestedProductsResult;
import com.architecture.first.framework.business.vicinity.tasklist.TaskTracking;
import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.security.events.UserAccessRequestEvent;
import com.architecture.first.framework.security.events.UserTokenReplyEvent;
import com.architecture.first.framework.security.model.Credentials;
import com.architecture.first.framework.security.model.SystemInfo;
import com.architecture.first.framework.security.model.UserToken;
import com.architecture.first.framework.technical.events.ActorProcessingErrorEvent;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.events.DefaultLocalEvent;
import com.architecture.first.framework.technical.util.SimpleModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.architecture.first.framework.business.hub.ClientCommunication.CLIENT;

@Slf4j
@Service
public class Customer extends BusinessActor {

    private final CustomerRepository repository;
    private final ClientCommunication client;

    @Autowired
    public Customer(CustomerRepository repository, ClientCommunication client) {
        this.repository = repository;
        this.client = client;
        setGeneration("1.0.2");
    }

    @Override
    protected void init() {
        super.init();

        registerBehavior("ViewProducts", Customer.hearViewProductsResponse);
        registerBehavior("ViewProduct", Customer.hearViewProductResponse);
        registerBehavior("SuggestedProducts", Customer.hearSuggestedProducts);
        registerBehavior("RequestPayment", Customer.hearPaymentRequest);
        registerBehavior("OrderConfirmation", Customer.hearOrderConfirmation);
    }

    @TaskTracking(task = "customer/ViewProducts", defaultParentTask = "customer/ViewProducts")
    public List<Product> viewProducts(ArchitectureFirstEvent localEvent, ShowProductsCriteria criteria) {
        SimpleModel returnData = new SimpleModel();

        var event = new ArchitectureFirstEvent(this, "ViewProductsEvent", name(), "Merchant")
                .shouldAwaitResponse(true)
                .initFromDefaultEvent(localEvent);
        event.payload().put("criteria", criteria);

        say(event, response -> {
            if (response.isNamed("ViewProductsEvent")) {
                var evt = response;
                returnData.put("products", evt.getPayloadListValueAs("products", new TypeToken<List<Product>>(){}.getType()));
                log.info("products have arrived");
                return true;
            }
            return false;
        },
        exception -> {
            log.error("Exception: ", exception);
            return true;
        });

        log.info("return products");
        return (List<Product>) returnData.get("products");
    }

    @TaskTracking(task = "customer/ViewProduct", defaultParentTask = "customer/ViewProduct")
    public CartItem viewProduct(ArchitectureFirstEvent localEvent, Long id) {
        SimpleModel returnData = new SimpleModel();

        var event = new ArchitectureFirstEvent(this, "ViewProductEvent", name(), "Merchant")
                .setPayloadValue("productId", id)
                .shouldAwaitResponse(true)
                .initFromDefaultEvent(localEvent);

        say(event, response -> {
            if (response.isNamed("ViewProductEvent")) {
                returnData.put("product", response.getPayloadValueAs("product", CartItem.class));
                log.info("the product has arrived");
                return true;
            }
            return false;
            },
            exception -> {
                log.error("Exception: ", exception);
                return true;
            });

        log.info("return product");
        return (CartItem) returnData.get("product");
    }

    //C02 - Add Product to Cart
    @TaskTracking(task = "customer/AddProductToCart", defaultParentTask = "customer/AddProductToCart")
    public Optional<CartItem> addProductToCart(ArchitectureFirstEvent localEvent, CartItem product) {
        Long userId = SecurityGuard.getUserId(localEvent.getAccessToken());
        var optCartItems = getShoppingCart(localEvent, userId);
        if (optCartItems.isPresent()) {
            var numUpdates = repository.addItemToExistingOrder(userId, product);
            validateChange(product, numUpdates,"E502", "Cannot add item to cart");
        }
        else {
            var customerInfo = repository.getCustomerInfo(userId);
            if (customerInfo.isPresent()) {
                var cart = new ShoppingCart();
                cart.init(customerInfo.get());
                cart.addItem(product);
                var numInserts = repository.createOrder(cart);
                validateChange(product, numInserts,"E503", "Cannot create cart");
            }
        }

        return Optional.of(product);
    }

    public Optional<ShoppingCart> getShoppingCart(ArchitectureFirstEvent localEvent, Long userId) {
        return repository.getShoppingCart(userId);
    }

    public Optional<List<CartItem>> getShoppingCartItems(ArchitectureFirstEvent localEvent, Long userId) {
        var optCart = getShoppingCart(localEvent, userId);
        if (optCart.isPresent()) {
            // sum up quantities of like items
            var cartItems = optCart.get().getItems().stream().collect(
                            Collectors.toMap(
                                    i -> i.getProductId(),
                                    Function.identity(),
                                    (s, a) -> {
                                        s.setQuantity(s.getQuantity() + a.getQuantity());
                                        return s;
                                    }
                            ))

                    .values().stream().toList();
            return Optional.of(cartItems);
        }

        return Optional.empty();
    }

    public Optional<List<CartItem>> getShoppingCartItems(ArchitectureFirstEvent localEvent) {
        Long userId = SecurityGuard.getUserId(localEvent.getAccessToken());
        return getShoppingCartItems(localEvent, userId);
    }

    public Optional<Order> getOrderPreview(ArchitectureFirstEvent localEvent, Long userId) {
        var order = repository.getOrderPreview(userId);
        if (order == null) {
            return Optional.empty();
        }

        var purchaseItems = sumPurchaseItems(order.getPurchaseItems());
        order.setPurchaseItems(purchaseItems);
        var totalUnitPrice = calculateTotalUnitPrice(purchaseItems);

        order.setShippingCost(calculateShippingCost(totalUnitPrice));
        order.setTotalPrice(totalUnitPrice.add(order.getShippingCost()));

        return Optional.of(order);
    }

    public Optional<Order> getOrderPreview(ArchitectureFirstEvent localEvent) {
        Long userId = SecurityGuard.getUserId(localEvent.getAccessToken());
        return getOrderPreview(localEvent, userId);
    }

    public Optional<List<PurchaseItem>> getOrderPreviewItems(ArchitectureFirstEvent localEvent, Long userId) {
        var optCart = getOrderPreview(localEvent, userId);
        if (optCart.isPresent()) {
            List<PurchaseItem> cartItems = sumPurchaseItems(optCart.get().getPurchaseItems());
            return Optional.of(cartItems);
        }

        return Optional.empty();
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

        cartItems.forEach(c -> c.setQuantity(quantities.get(c.getProductId())));
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

    public Optional<List<PurchaseItem>> getOrderPreviewItems(ArchitectureFirstEvent localEvent) {
        Long userId = SecurityGuard.getUserId(localEvent.getAccessToken());
        return getOrderPreviewItems(localEvent, userId);
    }

    public Long checkout(ArchitectureFirstEvent localEvent) {
        Long userId = SecurityGuard.getUserId(localEvent.getAccessToken());
        final Customer ths = (Customer) AopContext.currentProxy();
        if (repository.isCustomerRegistered(userId)) {
            return ths.checkoutAsRegisteredCustomer(localEvent, userId);
        }

        return ths.checkoutAsGuest(localEvent, userId);
    }

    public OrderConfirmation checkoutAndWait(ArchitectureFirstEvent localEvent) {
        Long userId = SecurityGuard.getUserId(localEvent.getAccessToken());
        final Customer ths = (Customer) AopContext.currentProxy();
        if (repository.isCustomerRegistered(userId)) {
            return ths.checkoutAsRegisteredCustomerAndWait(localEvent, userId);
        }

        return ths.checkoutAsGuestAndWait(localEvent, userId);
    }

    @TaskTracking(task = "customer/CheckoutAsRegisteredCustomer")
    public Long checkoutAsRegisteredCustomer(ArchitectureFirstEvent localEvent, Long userId) {
        var optCart = repository.getShoppingCart(userId);

        if (optCart.isEmpty()) {
            // TODO - create shopping cart if not available
            throw new ActorException(this, "CART_NOT_AVAILABLE");
        }

        var cart = optCart.get();
        var event = new ArchitectureFirstEvent(this, "CheckoutRequestEvent", name(), Arrays.asList("Merchant","Cashier"))
                .setPayloadValue("userId", cart.getUserId())
                .setPayloadValue("orderNumber", cart.getOrderNumber())
                .setPayloadValue("shoppingCart", cart)
                .initFromDefaultEvent(localEvent);

        say(event);

        return cart.getOrderNumber();
    }

    @TaskTracking(task = "customer/CheckoutAsRegisteredCustomer")
    public OrderConfirmation checkoutAsRegisteredCustomerAndWait(ArchitectureFirstEvent localEvent, Long userId) {
        var optCart = repository.getShoppingCart(userId);

        if (optCart.isEmpty()) {
            // TODO - create shopping cart if not available
            throw new ActorException(this, "CART_NOT_AVAILABLE");
        }

        SimpleModel returnData = new SimpleModel();

        var cart = optCart.get();
        var event = new ArchitectureFirstEvent(this, "CheckoutRequestEvent", name(), Arrays.asList("Merchant","Cashier"))
                .setPayloadValue("userId", cart.getUserId())
                .setPayloadValue("orderNumber", cart.getOrderNumber())
                .setPayloadValue("shoppingCart", cart)
                .setAsSecured()
                .shouldAwaitResponse(true)
                .setAwaitTimeoutSeconds(60)
                .initFromDefaultEvent(localEvent);

        say(event, response -> {
                    if (response.isNamed("OrderConfirmationEvent")) {
                        returnData.put("orderConfirmation", response.getPayloadValueAs("orderConfirmation", OrderConfirmation.class));
                        log.info("the order has been processed");
                        return true;
                    }
                    return false;
                },
                exception -> {
                    log.error("Exception: ", exception);
                    return true;
                });

        return (OrderConfirmation) returnData.get("orderConfirmation");
    }

    @TaskTracking(task = "customer/CheckoutAsGuest")
    public Long checkoutAsGuest(ArchitectureFirstEvent localEvent, Long userId) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @TaskTracking(task = "customer/CheckoutAsGuest")
    public OrderConfirmation checkoutAsGuestAndWait(ArchitectureFirstEvent localEvent, Long userId) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @TaskTracking(task = "customer/authenticate")
    public UserToken authenticate(UserAccessRequestEvent accessRequestEvent) {
        AtomicReference<UserToken> token = new AtomicReference<>();
        accessRequestEvent.shouldAwaitResponse(true);
        accessRequestEvent.setAwaitTimeoutSeconds(60);
        say(accessRequestEvent, r -> {
            if ( r instanceof UserTokenReplyEvent) {
                var evt = (UserTokenReplyEvent) r;
                token.set(evt.getCustomerToken());
            }
            return true;
        });

        return token.get();
    }

    /**
     * Sign up customer
     * Note: has side-effects
     * @param signUp
     * @return userId and updated CustomerSignUp
     */
    // C12 - Sign Up
    @TaskTracking(task = "customer/SignUp")
    public UserToken signUp(ArchitectureFirstEvent localEvent, CustomerSignUp signUp) {
        var rc = repository.registerCustomer(signUp);

        if (rc == -1) {
            return new UserToken().reject("Username already exists");
        }

        return authenticate((UserAccessRequestEvent) new UserAccessRequestEvent(this, name(), SecurityGuard.IDENTITY_PROVIDER)
                .setCredentials(new Credentials(signUp.getEmailAddress(), signUp.getPassword()))
                .setOriginalEvent(localEvent)
        );
    }

    // C06 - View Suggested Products
    @TaskTracking(task = "customer/ViewSuggestedProducts", defaultParentTask = "customer/ViewProduct")
    protected long viewSuggestedProducts(ArchitectureFirstEvent event) {
        // List<InventorySuggestedProductsResult> getSuggestedProducts()

        var suggestedProducts = new ArrayList<InventorySuggestedProductsResult>();
        Gson gson = new Gson();

        ((ArrayList<Object>) event.getPayloadValueAs("suggestedProducts", Object.class)).stream().forEach(hm -> {
            var a = (Map<String,Object>) hm;
            var str = gson.toJson(a);
            suggestedProducts.add(gson.fromJson(str, InventorySuggestedProductsResult.class));
        });

        log.info("suggested products size: " + suggestedProducts.size());
        event.header().put("path","hub/customer/suggested-products");
        event.setTo(ClientCommunication.CLIENT);
        event.payload().put("suggestedProducts", suggestedProducts);
        client.say(event);

        return suggestedProducts.size();
    }

    // C05 - View Order Confirmation
    @TaskTracking(task = "customer/ViewOrderConfirmation", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected OrderConfirmation viewOrderConfirmation(ArchitectureFirstEvent event) {
        log.info("order confirmation arrived for: " + event.getPayloadValueAs("userId", Long.class));
        var orderConfirmation = (OrderConfirmation) event.getPayloadValueAs("orderConfirmation", OrderConfirmation.class);

        event.header().put("path","hub/customer/order-confirmation");
        event.setTo(ClientCommunication.CLIENT);
        event.payload().put("orderConfirmation", orderConfirmation);
        client.say(event);

        return orderConfirmation;
    }

    // C04 - Provide Payment
    @TaskTracking(task = "customer/ProvidePayment", defaultParentTask = "customer/CheckoutAsRegisteredCustomer")
    protected void providePayment(ArchitectureFirstEvent requestPaymentEvent) {
        var userId = (Long) requestPaymentEvent.getPayloadValueAs("userId", Long.class);
        var orderNumber = (Long) requestPaymentEvent.getPayloadValueAs("orderNumber", Long.class);

        log.info("asked to provide payment for: " + userId);

        say( new ArchitectureFirstEvent(this, "PaymentResponseEvent", name(), requestPaymentEvent.from())
                .setPayloadValue("userId", userId)
                .setPayloadValue("orderNumber", orderNumber)
                .setPayloadValue("approvalStatus", true)
                .setAsSecured()
                .setOriginalEvent(requestPaymentEvent)
        );
    }

    protected long notifyOfProducts(ArchitectureFirstEvent event) {
        event.header().put("path","hub/customer/view-products");
        event.setTo(ClientCommunication.CLIENT);
        client.say(event);

        return 0;
    }

    protected CartItem notifyOfProduct(ArchitectureFirstEvent event) {
        event.header().put("path","hub/customer/view-product");
        event.setTo(ClientCommunication.CLIENT);
        client.say(event);

        return (CartItem) event.getPayloadValueAs("product", CartItem.class);
    }

    private SystemInfo validateChange(SystemInfo systemInfo, long numUpdates,
                                      String errorCode, String message) {
        if (numUpdates != 1) {
            systemInfo.setErrorCode(errorCode);
            systemInfo.setMessage(message);
        }
        return systemInfo;
    }

    protected static Function<ArchitectureFirstEvent, Actor> hearViewProductsResponse = (event -> {
        var evt = event;

        log.info("products have been received " + evt.getPayloadValue("products"));

        final Customer ths = (Customer) AopContext.currentProxy();
        ths.notifyOfProducts(evt);
        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearViewProductResponse = (event -> {
        log.info("product has been received " + event.getPayloadValueAs("product.productId", Long.class));

        final Customer ths = (Customer) AopContext.currentProxy();
        ths.notifyOfProduct(event);
        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearSuggestedProducts = (event -> {
        final Customer ths = (Customer) AopContext.currentProxy();
        ths.viewSuggestedProducts(event);

        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearPaymentRequest = (event -> {
        final Customer ths = (Customer) AopContext.currentProxy();

        ths.providePayment(event);

        return ths;
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearOrderConfirmation = (event -> {
        final Customer ths = (Customer) AopContext.currentProxy();

        ths.viewOrderConfirmation(event);

        return ths;
    });

    @Override
    public void onException(ArchitectureFirstEvent event, ActorException e, String message) {
        log.error((StringUtils.isNotEmpty(message)) ? message : "Error: ", e);
        var msg = (StringUtils.isNotEmpty(message)) ? message : e.getMessage();
        if (msg.contains("CART_NOT_AVAILABLE")) {
            msg = "CART_NOT_AVAILABLE You must add a product before checking out";
        }
        if (msg.contains("Exception:")) {   // Return a technical support message
            msg = String.format("%s %s.  Please contact the technical support team",
                    (StringUtils.isNotEmpty(event.getRequestId())) ? "RequestId: " + event.getRequestId() : "",
                    (e.getActor() != null) ? "/ " + e.getActor().name() : ""
            );
        }
        var clientEvent = new DefaultLocalEvent(event.getRequestId())
                .setOriginalEvent(event)
                .setTo(CLIENT)
                .setFrom(name());
        clientEvent.payload().put("error", msg);
        clientEvent.payload().put("status", 504);
        client.say(clientEvent);
    }

    @Override
    protected void onActorProcessingError(ActorProcessingErrorEvent event) {
        var msg = event.message();
        log.error(msg);

        if (msg.contains("Exception:")) {   // Return a technical support message
            msg = String.format("%s %s.  Please contact the technical support team",
                    (StringUtils.isNotEmpty(event.getRequestId())) ? "RequestId: " + event.getRequestId() : "",
                    (event.getTarget().isPresent() ? "/ " + event.getTarget().get().name() : "")
            );
        }
        var clientEvent = new DefaultLocalEvent(event.getRequestId())
                .setOriginalEvent(event)
                .setTo(CLIENT)
                .setFrom(name());
        clientEvent.payload().put("error", msg);
        clientEvent.payload().put("status", 505);
        client.say(clientEvent);
    }

    @Override
    protected void onUnhandledEvent(ArchitectureFirstEvent event) {
        // .. override to handle
        log.warn("unhandled event: " + event.toString(), event);
    }

    @Override
    protected void onTerminate(String reason) {
/*        super.onTerminate(reason);
        log.info("Terminating Customer: " + name());

        CustomerApplication.stop();*/
    }
}
