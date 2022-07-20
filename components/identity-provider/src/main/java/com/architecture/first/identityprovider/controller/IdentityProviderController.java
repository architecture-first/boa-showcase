package com.architecture.first.identityprovider.controller;

import com.architecture.first.framework.security.model.Credentials;
import com.architecture.first.framework.technical.user.UserInfo;
import com.architecture.first.identityprovider.actors.IdentityProvider;
import com.architecture.first.framework.security.model.UserToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/identity")
public class IdentityProviderController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private IdentityProvider securityGuard;

    @PostMapping("authenticate/customer")
    public UserToken authenticateCustomer(@RequestBody Credentials credentials) {
        return securityGuard.authenticateUser(credentials);
    }

    @PostMapping("validate/customer/token")
    public UserToken validateToken() {
        UserInfo info = (UserInfo) request.getAttribute("userInfo");
        UserToken userToken = new UserToken();
        userToken.setToken(info.getAccessToken());
        return securityGuard.validateToken(userToken);
    }
}
