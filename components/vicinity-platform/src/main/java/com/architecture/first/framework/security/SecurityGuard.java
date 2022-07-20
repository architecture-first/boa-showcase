package com.architecture.first.framework.security;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.vicinity.events.AcknowledgementEvent;
import com.architecture.first.framework.business.vicinity.events.AnonymousOkEvent;
import com.architecture.first.framework.business.vicinity.events.ErrorEvent;
import com.architecture.first.framework.business.vicinity.vault.Vault;
import com.architecture.first.framework.security.events.AccessRequestEvent;
import com.architecture.first.framework.security.events.SecurityIncidentEvent;
import com.architecture.first.framework.security.events.UserAccessRequestEvent;
import com.architecture.first.framework.security.events.UserTokenRequestEvent;
import com.architecture.first.framework.security.model.Credentials;
import com.architecture.first.framework.security.model.Token;
import com.architecture.first.framework.security.model.UserToken;
import com.architecture.first.framework.technical.config.AppContext;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.events.CheckupEvent;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.ApplicationContext;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * The in process security that interacts with the Vicinity
 */
@Slf4j
@Service
public class SecurityGuard extends Actor {
    @Autowired
    private static ApplicationContext applicationContext;

    @Autowired
    private Vault vault;

    public static final String SECURITY_GUARD = "SecurityGuard";
    public static final String VICINITY_MONITOR = "VicinityMonitor";
    public static final String IDENTITY_PROVIDER = "IdentityProvider";
    public static final String SECURITY_CUSTOMER = "bill.d.wahl@matrix.com";
    public static final Long SECURITY_USER_ID = 201l;
    public static final String SECURITY_TOKEN = "securityToken";

    // NOTE: This is only for demonstration and would be more secure in a real application
    @Value("${identity-provider.private-key}")
    private static String privateKey = "4pOb5z5ALQhlUJQKlKzTZyB01MxFRZ8hKBKarI0jpV8PVMvC0UxJFYHA2jVtqfj";
    private static SecretKey secretKey;
    private static String internalToken;

    private static final int requestIdSize = 20;
    public static boolean isWithIdentityProvider = false;

    /**
     * Creates an internal token for proactive async procesing
     */
    @Override
    protected void init() {
        super.init();
        setGeneration("1.0.2");

        secretKey = Keys.hmacShaKeyFor(Encoders.BASE64URL.encode(privateKey.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
        internalToken = vault.getItem(SECURITY_TOKEN);
    }

    /**
     * Perform processing on 24 hour mark.
     * Renew async token
     */
    @Override
    protected void on24hours() {
        // There is no need send request if in vicinity with IdentityProvider
        if (!isWithIdentityProvider && !isTokenValid(internalToken)) {
            renewToken();
        }
    }

    /**
     * Renew async token
     */
    protected void renewToken() {
        internalToken = vault.getItem(SECURITY_TOKEN);
        if (StringUtils.isNotEmpty(internalToken)) {
            try {
                if (isTokenValid(internalToken)) {
                    return;
                }
            }
            catch (ExpiredJwtException e) {
                // fall through to renew logic
            }
        }

        // Ask for new token
        if (StringUtils.isEmpty(internalToken)) {
            Credentials credentials = new Credentials(SECURITY_CUSTOMER, "");
            UserAccessRequestEvent accessRequestEvent = new UserAccessRequestEvent(this, name(), IDENTITY_PROVIDER)
                    .setCredentials(credentials);

            say(accessRequestEvent, r -> {
                internalToken = r.getProcessedJwtToken();
                return true;
            });

            return;
        }

        // Renew current token
        UserToken userToken = new UserToken();
        userToken.setUserId(SECURITY_USER_ID);
        userToken.setToken(internalToken);
        UserTokenRequestEvent tokenRequestEvent = new UserTokenRequestEvent(this, name(), IDENTITY_PROVIDER)
                .setUserToken(userToken);
        say(tokenRequestEvent, r -> {
            internalToken = r.getProcessedJwtToken();
            return true;
        });
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * Determines if the access token is valid
     * @param userToken
     * @return
     */
    public boolean isValid(UserToken userToken) {
        return validateJwtTokenFn.apply(userToken.getToken());
    }

    /**
     * Performs token validation
     */
    private final Function<String, Boolean> validateJwtTokenFn = (t -> {
        if (StringUtils.isEmpty(t)) {
            return false;
        }

        parseToken(t);

        return true;
    });

    /**
     * Parses the access token and returns internal claims
     * @param t
     * @return list of claims
     */
    private static Jws<Claims> parseToken(String t) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(t);

        return jws;
    }

    /**
     * Determines if the access token is valid
     * @param jwtToken
     * @return true if it is valid
     */
    public boolean isTokenValid(String jwtToken) {
        return validateJwtTokenFn.apply(jwtToken);
    }

    /**
     * Determines if the access token is valid
     * @param token
     * @return true if it is valid
     */
    public boolean isTokenValid(Token token) {
        return validateJwtTokenFn.apply(token.getToken());
    }

    /**
     * Determines if the user token is valid
     * @param userToken
     * @return true if it is valid
     */
    public boolean isTokenValid(UserToken userToken) {
        try {
            validateJwtTokenFn.apply(userToken.getToken());
            userToken.validated();
            return true;
        }
        catch (JwtException ex) {
            log.info("Invalid token encountered", ex);
            // It is ok to eat this exception
            userToken.reject(ex.getMessage());
            return false;
        }
    }

    /**
     * Returns the userid from the access token
     * @param jwtToken
     * @return userid
     */
    public static Long getUserId(String jwtToken) {
        var jws = parseToken(jwtToken);
        return ((Double)jws.getBody().get("userId")).longValue();
    }

    /**
     * Determines if the event passes validation and can be sent in the Vicinity
     * @param event
     * @return true if the event is valid
     */
    public static boolean isOkToProceed(ArchitectureFirstEvent event) {
        return event instanceof ErrorEvent || event instanceof AccessRequestEvent
                || event instanceof CheckupEvent || event instanceof AcknowledgementEvent
                || event instanceof AnonymousOkEvent
                || new SecurityGuard().isTokenValid(event.getAccessToken());
    }

    /**
     * Determines if the event needs an access token to proceed further
     * @param event
     * @return true if not one of the excluded events
     */
    public static boolean needsAnAccessToken(ArchitectureFirstEvent event) {
        return !isOkToProceed(event);
    }

    /**
     * Add a Token for an event that needs it
     * @param event
     */
    public static void prepareEvent(ArchitectureFirstEvent event) {
        if (isOkToProceed(event)) {
            return;
        }

        event.setAccessToken(internalToken);
    }

    /**
     * Reply to the original event
     * @param event
     * @return
     */
    public static ArchitectureFirstEvent replyToSender(ArchitectureFirstEvent event) {
        Actor actor = determineTargetActor(event);

        var incident = new SecurityIncidentEvent(actor, SECURITY_GUARD,  event.from(), event);

        return actor.say(incident);
    }

    /**
     * Determine actor that is targeted for the event
     * @param event
     * @return
     */
    private static Actor determineTargetActor(ArchitectureFirstEvent event) {
        Actor actor = (event.getSource() instanceof Actor)
                ? (Actor) event.getSource()
                : (event.getTarget() != null && event.getTarget().isPresent())
                    ? event.getTarget().get()
                    : AppContext.getBean(SecurityGuard.class);
        return actor;
    }

    /**
     * Report a security error for the event
     * @param event
     * @param message
     * @return
     */
    public static ArchitectureFirstEvent reportError(ArchitectureFirstEvent event, String message) {
        Actor actor = determineTargetActor(event);
        event.setHasErrors(true);

        var incident = new SecurityIncidentEvent(actor, SECURITY_GUARD,  VICINITY_MONITOR, event);
        incident.setMessage(message);
        incident.setHasErrors(true);

        return actor.announce(incident);
    }

    public static String getRequestId() {
        return RandomStringUtils.randomAlphanumeric(requestIdSize);
    }

    public static String getAccessToken() {
        return internalToken;
    }
}
