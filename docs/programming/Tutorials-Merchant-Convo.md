# Merchant Convo

## Prerequisite 

[Hello Actor Tutorial](Tutorials-Hello-Actor.md)

This tutorial will show the steps to create an Actor that speaks to the Merchant.

## Prerequisites

The retail showcase application is running as a Docker Compose application or in a local Kubernetes Cluster as described in [Getting Started](../../README.md).

## Steps

### Add dependencies to Hello Actor application

```xml
		<dependency>
            <groupId>com.architecture.first</groupId>
            <artifactId>business-retail</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
```

### Create Event Handler

```java
    protected static Function<ArchitectureFirstEvent, Actor> hearViewProductsResponse = (event -> {
        var evt = (ViewProductsEvent) event;

        System.out.println(event.payload());
        return event.getTarget().get();
    });
```

### Register Event Handler

```java
    @Override
    protected void init() {
        super.init();

        registerBehavior("Message", Trainee.hearMessageEvent);
        registerBehavior("ViewProducts", Trainee.hearViewProductsResponse); // <-- new handler
    }
```

### Full Class Definition

```java
package com.architecture.first.boa.helloactor.actors;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.retail.events.ViewProductEvent;
import com.architecture.first.framework.business.retail.events.ViewProductsEvent;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Trainee extends Actor {

    @Override
    protected void init() {
        super.init();

        registerBehavior("Message", Trainee.hearMessageEvent);
        registerBehavior("ViewProducts", Trainee.hearViewProductsResponse);
    }

    protected static Function<ArchitectureFirstEvent, Actor> hearMessageEvent = (event -> {
        System.out.println(event.payload().get("message"));

        return event.getTarget().get();
    });

    protected static Function<ArchitectureFirstEvent, Actor> hearViewProductsResponse = (event -> {
        var evt = (ViewProductsEvent) event;

        System.out.println(event.payload());
        return event.getTarget().get();
    });
}
```

### Add Test Case

```java
	@Test
	void testViewProducts() {
            var event = new ViewProductsEvent(this, trainee.name(), "Merchant")
            .setAsRequiresAcknowledgement(false);

            trainee.say(event);
            Assert.isTrue(true, "No testing needed :)");
        }
```

### Full Test Class
```java
package com.architecture.first.boa.helloactor;

import com.architecture.first.boa.helloactor.actors.Trainee;
import com.architecture.first.boa.helloactor.events.MessageEvent;
import com.architecture.first.framework.business.retail.events.ViewProductsEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

@SpringBootTest
class HelloActorApplicationTests {

    @Autowired
    private Trainee trainee;

    @Test
    void testHelloWorld() {
        var event = new MessageEvent(this, "Tester", "Trainee")
                .setAsRequiresAcknowledgement(false);
        event.payload().put("message", "hey there world");

        trainee.whisper(event);
        Assert.isTrue(true, "No testing needed :)");
    }

    @Test
    void testViewProducts() {
        var event = new ViewProductsEvent(this, trainee.name(), "Merchant")
                .setAsRequiresAcknowledgement(false);

        trainee.say(event);
        Assert.isTrue(true, "No testing needed :)");
    }

}
```

### Run the testViewProducts test

```shell
2022-07-18 21:03:46.985  INFO 5596 --- [  a1-vicinity-5] c.a.f.f.business.vicinity.Vicinity       : Received and Locally Published Event: {"products":[{"productId":1001,"name":"Rod\u0027s Fishing Rod","type":"Fishing Rod","imageUrl":"images/Fishing-Rod.png","attributes":["Color: Red","Color: White","Freshwater"],"price":[{"value":45.9900000000000,"type":"one-time","discounts":[{"name":"dollar-off","partOf":"Dollar Days Discount","value":1.00000000000000}]}],"unitsAvailable":105,"isActive":true,"updatedBy":"system"},{"productId":1002,"name":"Larry\u0027s Fishing Lure","type":"Fishing Lure","imageUrl":"images/Fishing-Lure.png","attributes":["Color: Grey","Freshwater","Bass"],"price":[{"value":9.99000000000000,"type":"one-time","discounts":[]}],"unitsAvailable":400,"isActive":true,"updatedBy":"system"},{"productId":1003,"name":"Frank\u0027s Fishing Boat","type":"Row Boat","imageUrl":"images/Fishing-Row-Boat.png","attributes":["Color: Blue","Freshwater","Oars"],"price":[{"value":300.990000000000,"type":"one-time","discounts":[{"name":"dollar-off","partOf":"Dollar Days Discount","value":5.00000000000000}]}],"unitsAvailable":40,"isActive":true,"updatedBy":"system"},{"productId":1004,"name":"Ray\u0027s Fishing Tool Repair","type":"Fishing Tool Repair","imageUrl":"images/Fishing-Repair-Tools.png","attributes":["Color: Grey","Screwdriver","Wrench"],"price":[{"value":25.9900000000000,"type":"one-time","discounts":[]}],"unitsAvailable":144,"isActive":true,"updatedBy":"system"}],"criteria":{"jsonCriteria":""},"header":{"from":"Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886","to":["Trainee.default.1.0.0.2055ab28-c810-4266-9f93-342cab81ddde"],"requestId":"jnfY3HUadTvmwfOqwZod","originalEventName":"ViewProductsEvent"},"payload":{},"message":"","isPropagatedFromVicinity":true,"isLocalEvent":false,"isAnnouncement":false,"wasHandled":false,"awaitResponse":false,"awaitTimeoutSeconds":30,"isPipelineEvent":false,"hasErrors":false,"isReply":true,"requiresAcknowledgement":false,"isToDoTask":false,"toDoLink":"","processLaterIfNoActorFound":true,"originalActorName":"","tasklist":"customer/ViewProducts","index":0,"timestamp":1658192626940}
2022-07-18 21:03:46.992  INFO 5596 --- [  a1-vicinity-5] c.a.f.framework.business.actors.Actor    : Receiving event: {"products":[{"productId":1001,"name":"Rod\u0027s Fishing Rod","type":"Fishing Rod","imageUrl":"images/Fishing-Rod.png","attributes":["Color: Red","Color: White","Freshwater"],"price":[{"value":45.9900000000000,"type":"one-time","discounts":[{"name":"dollar-off","partOf":"Dollar Days Discount","value":1.00000000000000}]}],"unitsAvailable":105,"isActive":true,"updatedBy":"system"},{"productId":1002,"name":"Larry\u0027s Fishing Lure","type":"Fishing Lure","imageUrl":"images/Fishing-Lure.png","attributes":["Color: Grey","Freshwater","Bass"],"price":[{"value":9.99000000000000,"type":"one-time","discounts":[]}],"unitsAvailable":400,"isActive":true,"updatedBy":"system"},{"productId":1003,"name":"Frank\u0027s Fishing Boat","type":"Row Boat","imageUrl":"images/Fishing-Row-Boat.png","attributes":["Color: Blue","Freshwater","Oars"],"price":[{"value":300.990000000000,"type":"one-time","discounts":[{"name":"dollar-off","partOf":"Dollar Days Discount","value":5.00000000000000}]}],"unitsAvailable":40,"isActive":true,"updatedBy":"system"},{"productId":1004,"name":"Ray\u0027s Fishing Tool Repair","type":"Fishing Tool Repair","imageUrl":"images/Fishing-Repair-Tools.png","attributes":["Color: Grey","Screwdriver","Wrench"],"price":[{"value":25.9900000000000,"type":"one-time","discounts":[]}],"unitsAvailable":144,"isActive":true,"updatedBy":"system"}],"criteria":{"jsonCriteria":""},"header":{"from":"Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886","to":["Trainee.default.1.0.0.2055ab28-c810-4266-9f93-342cab81ddde"],"requestId":"jnfY3HUadTvmwfOqwZod","originalEventName":"ViewProductsEvent"},"payload":{},"message":"","isPropagatedFromVicinity":true,"isLocalEvent":false,"isAnnouncement":false,"wasHandled":false,"awaitResponse":false,"awaitTimeoutSeconds":30,"isPipelineEvent":false,"hasErrors":false,"isReply":true,"requiresAcknowledgement":false,"isToDoTask":false,"toDoLink":"","processLaterIfNoActorFound":true,"originalActorName":"","tasklist":"customer/ViewProducts","index":0,"timestamp":1658192626940}
```

### Success

The default interactions are asynchronous, but you are free to use blocking code if necessary, such as for Controllers.
The example code for this is in the Customer class as shown below.

```java
    public List<Product> viewProducts(ArchitectureFirstEvent localEvent, ShowProductsCriteria criteria) {
        SimpleModel returnData = new SimpleModel();

        var event = new ViewProductsEvent(this, name(), "Merchant")
                .setCriteria(criteria)
                .setRequestId(localEvent.getRequestId())
                .setAccessToken(localEvent.getAccessToken())
                .shouldAwaitResponse(true)
                .initFromDefaultEvent(localEvent);

        say(event, response -> {
            if (response instanceof ViewProductsEvent) {
                var evt = (ViewProductsEvent) response;
                returnData.put("products", evt.getProducts());
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

```

The value of shouldAwaitResponse should be set to true in that case.

## Tutorials
[Hello Actor Tutorial](Tutorials-Hello-Actor.md)

## Links
- [Overview](Overview.md 'Overview')
- [Concepts](Concepts.md)
- [Vicinity Features](Vicinity-Features.md 'Vicinity Features')
- [Actor Features](Actor-Features.md)
- [Messaging](Messaging.md)
- [Special Features](Special-Features.md)
- [Troubleshooting](Troubleshooting.md)
- [Tips and Tricks](Tips-and-Tricks.md)
- [Getting Started](../../README.md)
