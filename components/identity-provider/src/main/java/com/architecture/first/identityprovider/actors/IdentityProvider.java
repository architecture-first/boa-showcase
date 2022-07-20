package com.architecture.first.identityprovider.actors;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.vicinity.vault.Vault;
import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.security.events.UserAccessRequestEvent;
import com.architecture.first.framework.security.events.UserTokenReplyEvent;
import com.architecture.first.framework.security.events.UserTokenRequestEvent;
import com.architecture.first.framework.security.model.Credentials;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.security.model.UserToken;
import com.architecture.first.identityprovider.IdentityProviderApplication;
import com.architecture.first.identityprovider.repository.IdentityRepository;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import static com.architecture.first.framework.security.SecurityGuard.SECURITY_USER_ID;

@Slf4j
@Service
public class IdentityProvider extends Actor {

    private final IdentityRepository userList;
    private final SecurityGuard securityGuard;
    private final Vault vault;

    @Value("${identity-provider.private-key}")
    private String privateKey;

    @Value("${identity-provider.expiration-days:180}")
    private int expirationDays;

    public static final String SECURITY_CUSTOMER = "bill.d.wahl@matrix.com";
    public static final String SECURITY_TOKEN = "securityToken";

    static {
        SecurityGuard.isWithIdentityProvider = true;
    }

    @Autowired
    public IdentityProvider(IdentityRepository identityRepository, SecurityGuard securityGuard,
                            Vault vault) {
        userList = identityRepository;
        this.securityGuard = securityGuard;
        this.vault = vault;
        setGeneration("1.0.2");
    }

    @Override
    protected void init() {
        super.init();

        registerBehavior("UserTokenRequest", IdentityProvider.noticeUserTokenRequest);
        registerBehavior("UserAccessRequest", IdentityProvider.noticeUserAccessRequest);

        renewInternalToken();
    }


    private void renewInternalToken() {
        var jwtToken = generateJwtToken(SECURITY_USER_ID, "");
        vault.addItem(SECURITY_TOKEN, jwtToken);
    }

    /**
     * Perform processing on 24 hour mark
     */
    @Override
    protected void on12hours() {
        renewInternalToken();
    }

    // I01 - Authenticate Customer
    public UserToken authenticateUser(Credentials credentials) {
        // Note: replace with custom authentication technique as this is only for demonstration

        var optToken = userList.findUserId(credentials.getUsername());

        if (optToken.isPresent()) {
            var token = optToken.get();
            return (token.isIdentityFound())
                    ? token.approve(generateJwtToken(token.getUserId(), token.getFirstName()))
                    : token.reject("User " + credentials.getUsername() + " was not found");
        }

        return new UserToken().reject("user entry not found");
    }

    public UserToken validateToken(UserToken token) {
        securityGuard.isTokenValid(token);
        return token;
    }

    protected String generateJwtToken(Long userId, String firstName) {
        return Jwts.builder()
                .setIssuer("RetailApp")
                .setSubject("access")
                .claim("userId", userId)
                .claim("name", firstName)
                .claim("scope", "customer")
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(Period.ofDays(expirationDays))))
                .signWith(securityGuard.getSecretKey())
                .compact();

    }

    // I01 - Authenticate Customer
    protected UserToken authenticateUser(UserToken userToken) {
        // Note: replace with custom authentication technique

        // check if customer token is valid
        if (!securityGuard.isTokenValid(userToken)) {
            log.info("Token is rejected: " + userToken.getToken());
            return userToken.reject("Token is invalid");
        }

        // issue a replace token
        return new UserToken().approve(
                generateJwtToken(userToken.getUserId(), userToken.getFirstName())
        );
    }

    protected static Function<ArchitectureFirstEvent, Actor> noticeUserTokenRequest = (event -> {
        UserTokenRequestEvent evt = (UserTokenRequestEvent) event;
        IdentityProvider identityProvider = (IdentityProvider) event.getTarget().get();
        Map<String, Object> customerTokenMap = (Map<String, Object>) evt.payload().get("token");
        UserToken originalUserToken = UserToken.from(customerTokenMap);

        UserToken userToken = identityProvider.authenticateUser(originalUserToken);
        userToken.setUserId(originalUserToken.getUserId());

        identityProvider.say(new UserTokenReplyEvent(identityProvider, identityProvider.name(), event.from())
                .setCustomerToken(userToken)
                .setOriginalEvent(event));

        return identityProvider;
    });

    protected static Function<ArchitectureFirstEvent, Actor> noticeUserAccessRequest = (event -> {
        UserAccessRequestEvent evt = (UserAccessRequestEvent) event;
        IdentityProvider identityProvider = (IdentityProvider) event.getTarget().get();
        Map<String, Object> customerTokenMap = (Map<String, Object>)  evt.payload().get("credentials");
        Credentials credentials = new Credentials((String) customerTokenMap.get("username"), (String) customerTokenMap.get("password"));

        UserToken userToken = identityProvider.authenticateUser(credentials);

        identityProvider.say(new UserTokenReplyEvent(identityProvider, identityProvider.name(), event.from(), event)
                .setCustomerToken(userToken));

        return identityProvider;
    });

    @Override
    protected void onTerminate(String reason) {
        super.onTerminate(reason);
        log.info("Terminating IdentityProvider: " + name());

        IdentityProviderApplication.stop();
    }
}
