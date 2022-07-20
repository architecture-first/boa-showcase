package com.architecture.first.framework.business.retail.aop;

import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.security.model.Token;
import com.architecture.first.framework.technical.user.UserInfo;
import io.jsonwebtoken.ExpiredJwtException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomerHttpHeaderFilter extends OncePerRequestFilter {

    @Autowired
    SecurityGuard securityGuard;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (!request.getServletPath().startsWith("/api/identity/authenticate/customer") &&
                !request.getServletPath().startsWith("/api/customer/authenticate") &&
                !request.getServletPath().startsWith("/api/customer/signUp") &&
                !request.getServletPath().contains("/product") &&
                !request.getServletPath().contains("/favicon.ico") &&
                !request.getServletPath().startsWith("/error")) {
            if (!processUserToken(true, request, response)) {
                return;
            }
        }
        else {
            if (!processUserToken(false, request, response)) {
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean processUserToken(boolean isSecurePage, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String jwtToken = request.getHeader("Authorization");
        if (jwtToken == null) {
            if (isSecurePage) {
                handleAuthorizationError(response, "Missing token");
                return false;
            }
            return true;
        }

        jwtToken = jwtToken.replace("Bearer ", "");

        if ((StringUtils.isEmpty(jwtToken) || jwtToken.equals("undefined"))) {
            if (!isSecurePage) {
                return true;
            }
        }

        // if get this far then validate the token passed even if not a secured page
        try {
            if (!securityGuard.isTokenValid(jwtToken)) {
                handleAuthorizationError(response, "Invalid token");
                return false;
            }
        }
        catch (ExpiredJwtException e) {
            handleAuthorizationError(response, e.getMessage());
            return false;
        }
        Long userId = SecurityGuard.getUserId(jwtToken);

        Token token = new Token(SecurityGuard.getRequestId(), jwtToken);
        var userInfo = new UserInfo("", userId, token);

        String source = request.getHeader("source");
        if (StringUtils.isNotEmpty(source)) {
            userInfo.setSource(source);
        }
        request.setAttribute("userInfo", userInfo);
        request.setAttribute("requestId", token.getRequestId());
        return true;
    }

    private void handleAuthorizationError(HttpServletResponse response, String additionalMessage) throws IOException {
        response.setStatus(401); // TODO: Configure service to return unauthorized
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String msg = String.format("{ \"status\": 401, \"error\": \"Not Authorized  %s\"}",
                StringUtils.isNotEmpty(additionalMessage) ? additionalMessage : "");
        response.getWriter().write(msg);
    }

    private void handleAuthorizationError(HttpServletResponse response) throws IOException {
        handleAuthorizationError(response, "");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
