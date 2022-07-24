# Tips and Tricks

This document contains a list of tips and tricks to effectively use the platform.

## Use the Architecture-First strategy to identify Actors.

[Architecture-First Approach](https://tonymarius.substack.com/p/architecture-first-strategy?utm_source=url).

## Use the platform as a starting point.

It is recommended to modify the showcase retail platform to match the desired architecture.
Once, the Actors have been defined they can be slowly introduced into the showcase application and the irrelevant Actors replaced.

## Install Redis Commander

Note: A prerequisite for installing [Redis](https://nodejs.org/en/) Commander is NodeJs unless Redis Commander is installed via Docker.
[Redis Commander](https://www.npmjs.com/package/redis-commander) will help in interrogating the environment.

It is especially helpful for viewing the Bulletin board, Convos, Ack/UnAck messages, and To-Do lists.

To Run Redis Commander for local Docker Compose via NodeJs issue the following command:

```shell
npx redis-commander -p 9901 --redis-port 16379
```

To Run Redis Commander for local Kubernetes via NodeJs issue the following command:

```shell
npx redis-commander -p 9901 --redis-port 30389
```

![](images/Tips-And-Tricks/Tips-And-Tricks-Redis-Commander.png)

## Pay attention to the Request ID

The request ID ties all the related messages together as well as the Task lists.

```json
  "header": {
    "from": "Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886",
    "to": [
      "Trainee.default.1.0.0.2055ab28-c810-4266-9f93-342cab81ddde"
    ],
    "requestId": "jnfY3HUadTvmwfOqwZod",
    "originalEventName": "ViewProductsEvent"
  }
```
The request ID "jnfY3HUadTvmwfOqwZod" is shown above.

```shell
boa-retail-redis-service:6389> keys jnf*
```
The items to look for can be filtered with the keys command in redis-cli. 

```shell
 "jnfY3HUadTvmwfOqwZod/Ack"
 "jnfY3HUadTvmwfOqwZod/Convo"
 "jnfY3HUadTvmwfOqwZod/Tasklist:customer/ViewProduct"
```

These items can be investigated by using the hgetall command.

```shell
boa-retail-redis-service:6389> hgetall "jnfY3HUadTvmwfOqwZod/Tasklist:customer/ViewProduct"
```

Results

```shell
boa-retail-redis-service:6389> hgetall "jnfY3HUadTvmwfOqwZod/Tasklist:customer/ViewProduct"
 1) "customer/ViewSuggestedProducts"
 2) "{\"status\":\"Complete\",\"message\":\"Customer.default.1.0.2.93864d44-f785-4229-81cb-4492f597d302\",\"timeStamp\":\"2022-07-19T21:01:53.880972010Z[GMT]\"}"
 3) "numTasksCompleted"
 4) "4"
 5) "status"
 6) "Complete"
 7) "merchant/SuggestProducts"
 8) "{\"status\":\"Complete\",\"message\":\"Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886\",\"timeStamp\":\"2022-07-19T21:01:53.872385394Z[GMT]\"}"
 9) "lastCompletedTask"
10) "customer/ViewProduct"
11) "merchant/ShowProduct"
12) "{\"status\":\"Complete\",\"message\":\"Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886\",\"timeStamp\":\"2022-07-19T21:01:53.874134758Z[GMT]\"}"
13) "customer/ViewProduct"
14) "{\"status\":\"Complete\",\"message\":\"Customer.default.1.0.2.93864d44-f785-4229-81cb-4492f597d302\",\"timeStamp\":\"2022-07-19T21:01:53.883379397Z[GMT]\"}"
```

The task list above shows the interactions for the task.

```shell
boa-retail-redis-service:6389> hgetall  "jnfY3HUadTvmwfOqwZod/Convo"
1) "{\"subject\":\"ViewProductEvent\",\"from\":\"Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886\",\"to\":\"Customer.default.1.0.2.93864d44-f785-4229-81cb-4492f597d302\",\"index\":\"0\"}"
2) "AcknowledgedByActor"
3) "{\"subject\":\"ViewProductEvent\",\"from\":\"Customer.default.1.0.2.93864d44-f785-4229-81cb-4492f597d302\",\"to\":\"Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886\",\"index\":\"0\"}"
4) "AcknowledgedByActor"
```
The Convo above shows the message passed through both Actors.

```shell
boa-retail-redis-service:6389> hgetall "jnfY3HUadTvmwfOqwZod/Ack"
1) "0"
2) "{\"header\":{\"to\":\"Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886\",\"from\":\"Customer.default.1.0.2.93864d44-f785-4229-81cb-4492f597d302\",\"eventType\":\"com.architecture.first.framework.business.retail.events.ViewProductEvent\"},\"jsonPayload\":\"{\\\"header\\\":{\\\"from\\\":\\\"Customer.default.1.0.2.93864d44-f785-4229-81cb-4492f597d302\\\",\\\"to\\\":[\\\"Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886\\\"],\\\"requestId\\\":\\\"jnfY3HUadTvmwfOqwZod\\\",\\\"originalEventName\\\":\\\"ViewProductEvent\\\",\\\"jwtToken\\\":\\\"eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJSZXRhaWxBcHAiLCJzdWIiOiJhY2Nlc3MiLCJ1c2VySWQiOjEwMDMsIm5hbWUiOiJsYW1vbnQiLCJzY29wZSI6ImN1c3RvbWVyIiwiaWF0IjoxNjU4MDkyMzA2LCJleHAiOjE2NTg2OTcxMDZ9.nwh5TasJqDaUaVFh4p9W7VbXVY4VTry-xpihvBAspTK0wMBQraoqzNhTQaZ8C78wGd0ciPPbU-Ps7L6-zJ_xjQ\\\",\\\"boa-conn\\\":\\\"icwtvdoEHE1658092284619\\\",\\\"boa-project\\\":\\\"holiday01\\\"},\\\"payload\\\":{\\\"productId\\\":1002},\\\"message\\\":\\\"\\\",\\\"isPropagatedFromVicinity\\\":true,\\\"isLocalEvent\\\":false,\\\"isAnnouncement\\\":false,\\\"wasHandled\\\":true,\\\"awaitResponse\\\":false,\\\"awaitTimeoutSeconds\\\":30,\\\"isPipelineEvent\\\":false,\\\"hasErrors\\\":false,\\\"isReply\\\":false,\\\"requiresAcknowledgement\\\":false,\\\"isToDoTask\\\":false,\\\"toDoLink\\\":\\\"\\\",\\\"processLaterIfNoActorFound\\\":true,\\\"originalActorName\\\":\\\"\\\",\\\"tasklist\\\":\\\"\\\",\\\"index\\\":0,\\\"timestamp\\\":1658264513857}\"}"
```

The Ack list shows the event that was acknowledged.

```shell
boa-retail-redis-service:6389> hgetall "BulletinBoard:topic/VicinityStatus/Merchant:2022-07-19/Active"
1) "Merchant.default.1.0.2.65e08755-0420-459e-bb27-75cdebde2886"
2) "{\"status\":\"Active\",\"subject\":\"status\",\"message\":\"running\",\"timestamp\":\"2022-07-19T21:13:00.026944684Z[GMT]\"}"
```

The bulletin board shows that the Actor with the expected ID is running and has a recent timestamp.

## If Redis is not available the Actors react

By default, if Redis is not available, the Actors will notice by their health check every minute.

```java
    /**
     * Perform internal tasks such as, analysis, inspection and cleanup
     */
    @Scheduled(cron = "0 * * * * *")
    protected void think() {
        try {
        . . .
            checkHealth();
            onThink();  // do proactive processing
        }
        catch (Exception e) {
            this.onException(new ActorException(this, e));
        }
    }
```

A health check is performed every minute.

```java
    protected void checkHealth() {
        if (!isHealthOk()) {
            onTerminate("self health not ok for: " + name());
        }

        if (!isVicinityHealthOk()) {
            onTerminate("vicinity connections are stale for: " + name());
        }

        if (!isEnvironmentOk()) {
            onTerminate("environment is invalid for: " + name());
        }

        giveStatus((!isAway)
                ? BulletinBoardStatus.Status.Active
                : BulletinBoardStatus.Status.Away, "running");
    }
```

If the environment is not stable the Actors will self terminate.
The monitoring tool can use this to take action to restart Redis and the Actors.

## Debug cluster code by using the boa-project technique.

Messages can be redirected to a locally running instance, possibly in IntelliJ by using [boa-project](Special-Features.md#bluegreen-deployment).


## Use a Controller for REST support

```java
@RestController
@RequestMapping("api/customer")
public class CustomerController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private Customer customer;

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
}
```

## Links
- [Overview](Overview.md 'Overview')
- [Concepts](Concepts.md)
- [Vicinity Features](Vicinity-Features.md 'Vicinity Features')
- [Actor Features](Actor-Features.md)
- [Messaging](Messaging.md)
- [Special Features](Special-Features.md)
- [Troubleshooting](Troubleshooting.md)
- [Getting Started](../../README.md)

## Tutorials
- [Tutorial: Hello Actor](Tutorials-Hello-Actor.md)
- [Tutorial: Merchant Convo](Tutorials-Merchant-Convo.md)