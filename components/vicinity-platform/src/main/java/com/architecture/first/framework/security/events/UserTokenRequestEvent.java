package com.architecture.first.framework.security.events;

import com.architecture.first.framework.security.model.UserToken;

import java.util.List;
import java.util.Map;

/**
 * Represents a user token request for internal processing
 */
public class UserTokenRequestEvent extends TokenRequestEvent implements AccessRequestEvent{
    public UserTokenRequestEvent(Object source, String from, List<String> to) {
        super(source, from, to);
    }

    public UserTokenRequestEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public UserTokenRequestEvent setUserToken(UserToken token) {
        this.payload().put("token", token);
        return this;
    }

    public UserToken getCustomerToken() {
        return UserToken.from((Map<String,Object>) this.payload().get("token"));
    }
}
