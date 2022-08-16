package com.architecture.first.merchant.actors;

import com.architecture.first.framework.business.retail.events.*;
import com.architecture.first.framework.business.retail.storefront.model.IProduct;
import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.security.events.UserTokenReplyEvent;
import com.architecture.first.framework.security.events.UserTokenRequestEvent;
import com.architecture.first.framework.security.model.UserToken;
import com.architecture.first.framework.technical.aop.RequestContext;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.events.BeginTerminationEvent;
import com.architecture.first.framework.technical.events.DefaultLocalEvent;
import com.architecture.first.framework.technical.user.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

//@ExtendWith(SpringExtension.class)
@RunWith(SpringRunner.class)
@SpringBootTest
class MerchantTests {

    @Autowired
    RequestContext requestContext;

    @Autowired
    MerchantForTesting merchant;

    @Autowired
    private ApplicationEventPublisher publisher;

    @BeforeEach
    void setupContext() {
        var userInfo = new UserInfo("",10001l, null);
        requestContext.setUserInfo(userInfo);

        String requestId = SecurityGuard.getRequestId();
        requestContext.setRequestId(requestId);
    }

    @Test
    void showProducts() {
        List<? extends IProduct> products = merchant.showProducts(new DefaultLocalEvent(requestContext.getRequestId()));
        Assert.notEmpty(products, "Products are empty");
    }

    @Test
    void showProduct() {
        var product = merchant.showProduct(new ArchitectureFirstEvent(this, "ViewProductEvent", "Tester", "Merchant")
                .setPayloadValue("productId", 1002l));
        Assert.notNull(product, "Product is empty");
    }

    @Test
    void reviewInventory() {
        Boolean hasChecked = merchant.reviewInventory(new ArchitectureFirstEvent(this, "ReviewInventoryEvent", "tester", "Merchant"));
        Assert.isTrue(hasChecked, "Inventory is not checked");
    }

    @Test
    void rememberAndRecall() {
        merchant.remember("aaa", "bbb");
        merchant.remember("ccc", "ddd");
        merchant.remember("num1", 21, Integer.class);
        merchant.remember("num2", 24, Integer.class);

        Integer num1 = merchant.recall("num1", Integer.class).get();
        Object num2 = merchant.recall("num2").get();
        Assert.notNull(num2, "Unable to recall memory");
    }

    @Test
    void brainDump() {
        merchant.remember("aaa", "bbb");
        merchant.remember("ccc", "ddd");
        merchant.remember("num1", 21, Integer.class);
        merchant.remember("num2", 24, Integer.class);

        Map<String, Object> facts = merchant.provideBrainDump();
        Assert.notEmpty(facts, "Unable to recall memory");
    }

    @Test
    void beginTermination() {
        BeginTerminationEvent event = new BeginTerminationEvent("Source", "tester", "Merchant");
        publisher.publishEvent(event);
        Assert.isTrue(true, "Inventory is not checked");
    }

    Function<ArchitectureFirstEvent, Boolean> fnTestReplyBehavior = (event -> {
        event.setMessage("The data is not correct");
        return true;
    });

    @Test
    void reply() {
        var event = new ArchitectureFirstEvent ("Source", "RequestBonusPointsEvent", "Customer", "Merchant");
        event.setHeaderValue("jwtToken", "eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJSZXRhaWxBcHAiLCJzdWIiOiJhY2Nlc3MiLCJjdXN0b21lcklkIjoiQzAwMSIsIm5hbWUiOiJCb2IiLCJzY29wZSI6ImN1c3RvbWVyIiwiaWF0IjoxNjUwNzQzOTYzLCJleHAiOjE2NTEzNDg3NjN9.rCRW26tlmMnb79thhQG5kI1uW68G36HVH0ap-JfW4Auaa_vtk2VW7j_IgHgtoGaSw3LCqsk1IeNIbu0zcGmyQA");
        event.setPayloadValue("userId", 1001l);
        event.setPayloadValue("orderNumber", 100001l);

        merchant.reply(event, fnTestReplyBehavior);
        Assert.isTrue(true, "");
    }

    @Test
    void requestNewToken() {
        UserToken userToken = new UserToken();
        userToken.setUserId(1001l);
        userToken.setToken("eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJSZXRhaWxBcHAiLCJzdWIiOiJhY2Nlc3MiLCJjdXN0b21lcklkIjoiQzAwMSIsIm5hbWUiOiJCb2IiLCJzY29wZSI6ImN1c3RvbWVyIiwiaWF0IjoxNjUwNzQzOTYzLCJleHAiOjE2NTEzNDg3NjN9.rCRW26tlmMnb79thhQG5kI1uW68G36HVH0ap-JfW4Auaa_vtk2VW7j_IgHgtoGaSw3LCqsk1IeNIbu0zcGmyQA");
        UserTokenRequestEvent event = new UserTokenRequestEvent("Source", merchant.name(), "IdentityProvider")
                .setUserToken(userToken);

        // say "to->IdentityProvider: 'requestToken(#currentToken)'"
        AtomicReference<String> jwtToken = new AtomicReference<>();
        merchant.say(event, e -> {
            UserTokenReplyEvent evt = (UserTokenReplyEvent) e;
            String token = evt.getCustomerToken().getToken();
            jwtToken.set(token);
            return true;
        });

        try {
            TimeUnit.SECONDS.sleep(20);
        }
        catch (InterruptedException e) {
            System.out.println("woke");
        }
    }

    private Function<ArchitectureFirstEvent, Object> processCustomerToken(AtomicReference<String> jwtToken) {
        return e -> {
            UserTokenReplyEvent evt = (UserTokenReplyEvent) e;
            String token = evt.getCustomerToken().getToken();
            jwtToken.set(token);
            return token;
        };
    }
}
