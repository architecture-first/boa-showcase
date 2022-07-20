package com.architecture.first.framework.business.retail.model.customer.controller;

import com.architecture.first.framework.business.actors.exceptions.ActorException;
import com.architecture.first.framework.business.retail.model.cashier.model.inventory.Product;
import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.Order;
import com.architecture.first.framework.business.retail.model.cashier.model.order.cart.PurchaseItem;
import com.architecture.first.framework.business.retail.model.cashier.model.order.confirmation.OrderConfirmation;
import com.architecture.first.framework.business.retail.model.criteria.ShowProductsCriteria;
import com.architecture.first.framework.business.retail.model.customer.CustomerSignUp;
import com.architecture.first.framework.business.retail.model.customer.actors.Customer;
import com.architecture.first.framework.business.retail.model.customer.cart.CartItem;
import com.architecture.first.framework.business.retail.model.customer.cart.ShoppingCart;
import com.architecture.first.framework.business.retail.model.customer.model.CustomerContext;
import com.architecture.first.framework.security.events.UserAccessRequestEvent;
import com.architecture.first.framework.security.model.Credentials;
import com.architecture.first.framework.security.model.UserToken;
import com.architecture.first.framework.technical.events.DefaultLocalEvent;
import com.architecture.first.framework.technical.user.UserInfo;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.architecture.first.framework.security.SecurityGuard.IDENTITY_PROVIDER;

@RestController
@RequestMapping("api/customer")
public class CustomerController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private Customer customer;

    @Autowired
    private ApplicationEventPublisher publisher;

    @PostMapping("products")
    public List<Product> getProducts(@RequestBody Product filter) {
        var criteria = new ShowProductsCriteria(
                new Gson().toJson(filter, Product.class)
        );

        return customer.viewProducts(new DefaultLocalEvent((String) request.getAttribute("requestId"))
                .addHeader("boa-conn", request.getHeader("boa-conn"))
                .addHeader("boa-project", request.getHeader("boa-project"))

                .setAccessToken(request.getAttribute("userInfo") != null
                        ? ((UserInfo) request.getAttribute("userInfo")).getAccessToken() : null), // non-secure page
                criteria);
    }

    @PostMapping("product")
    public CartItem getProduct(@RequestBody Product filter) {
        return customer.viewProduct(
                new DefaultLocalEvent((String) request.getAttribute("requestId"))
                        .addHeader("boa-conn", request.getHeader("boa-conn"))
                        .addHeader("boa-project", request.getHeader("boa-project"))

                        .setAccessToken(request.getAttribute("userInfo") != null
                                ? ((UserInfo) request.getAttribute("userInfo")).getAccessToken() : null), // non-secure page
                filter.getProductId()
        );
    }

    @PostMapping("cart/addProduct")
    public CartItem addProductToCart(@RequestBody CartItem item) {
        var optItem = customer.addProductToCart(new DefaultLocalEvent((String) request.getAttribute("requestId"))
                .setAccessToken(((UserInfo) request.getAttribute("userInfo")).getAccessToken())
                .addHeader("boa-project", request.getHeader("boa-project"))
                .addHeader("boa-conn", request.getHeader("boa-conn")), item);

        return optItem.isPresent() ? optItem.get() : null;
    }

    @PostMapping("cart")
    public Order getCart() {
        var optShoppingCart = customer.getOrderPreview(new DefaultLocalEvent((String) request.getAttribute("requestId"))
                .setAccessToken(((UserInfo) request.getAttribute("userInfo")).getAccessToken())
                .addHeader("boa-project", request.getHeader("boa-project"))
                .addHeader("boa-conn", request.getHeader("boa-conn")));

        return optShoppingCart.isPresent() ? optShoppingCart.get() : new Order();
    }

    @PostMapping("checkout")
    public Long checkout() {
        try {
            var orderNumber = customer.checkout(new DefaultLocalEvent((String) request.getAttribute("requestId"))
                                                        .addHeader("boa-conn", request.getHeader("boa-conn"))
                                                        .addHeader("boa-project", request.getHeader("boa-project"))
                                                        .setAccessToken(((UserInfo)request.getAttribute("userInfo")).getAccessToken()));
            return orderNumber;
        }
        catch (ActorException e) {
            return -99l;
        }
    }

    @PostMapping("checkoutAndWait")
    public OrderConfirmation checkoutAndWait() {
        try {
            var orderNumber = customer.checkoutAndWait(new DefaultLocalEvent((String) request.getAttribute("requestId"))
                                                                        .addHeader("boa-conn", request.getHeader("boa-conn"))
                                                                        .addHeader("boa-project", request.getHeader("boa-project"))
                                                                        .setAccessToken(((UserInfo)request.getAttribute("userInfo")).getAccessToken()));

            return orderNumber;
        }
        catch (ActorException e) {
            var oc = new OrderConfirmation();
            oc.setEmailAddress("Error-101"); // TODO - determine error handling approach
            return oc;
        }
    }

    @PostMapping("authenticate")
    public UserToken authenticateCustomer(@RequestBody Credentials credentials) {
        UserAccessRequestEvent accessRequestEvent = new UserAccessRequestEvent(this, customer.name(), IDENTITY_PROVIDER)
                .setCredentials(credentials);
                accessRequestEvent.addHeader("boa-conn", request.getHeader("boa-conn"));
                accessRequestEvent.addHeader("boa-project", request.getHeader("boa-project"));
        return customer.authenticate(accessRequestEvent);
    }

    @PostMapping("signUp")
    public UserToken signUp(@RequestBody CustomerSignUp signUp) {
        var token = customer.signUp(new DefaultLocalEvent((String) request.getAttribute("requestId"))
                .addHeader("boa-project", request.getHeader("boa-project"))
                .addHeader("boa-conn", request.getHeader("boa-conn")), signUp);

        return token;
    }
}
