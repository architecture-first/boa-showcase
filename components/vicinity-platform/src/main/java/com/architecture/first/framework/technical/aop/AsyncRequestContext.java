package com.architecture.first.framework.technical.aop;

import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.technical.user.UserInfo;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Request context for async requests
 */
@Component()
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AsyncRequestContext {

    public static final long DEFAULT_CUSTOMER_ID = 99;
    private final Map<String,RequestContext> requestContexts = new HashMap<>();

    protected RequestContext initContext(String name) {
        RequestContext context = new RequestContext();
        context.setRequestId(SecurityGuard.getRequestId());

        var userInfo = new UserInfo();
        userInfo.setUserId(DEFAULT_CUSTOMER_ID);

        String accessToken = SecurityGuard.getAccessToken();
        userInfo.setAccessToken(accessToken);
        context.setUserInfo(userInfo);

        context.setAsync(true);

        requestContexts.put(name, context);

        return context;
    }

    public void addContext(String name, RequestContext context) {
        requestContexts.put(name, context);
    }

    public RequestContext clearContext() {
        return requestContexts.remove(Thread.currentThread().getName());
    }

    public RequestContext requestContext() {
        String name = Thread.currentThread().getName();
        if (!requestContexts.containsKey(name)) {
            return initContext(name);
        }

        return requestContexts.get(name);
    }
}
