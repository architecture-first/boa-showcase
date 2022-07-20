package com.architecture.first.framework.technical.aop;

import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.security.model.Token;
import com.architecture.first.framework.technical.user.UserInfo;
import io.jsonwebtoken.ExpiredJwtException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Perform servlet level filtering and context setup
 */
@Component
public class UserHttpHeaderFilter extends OncePerRequestFilter {

    @Autowired
    SecurityGuard securityGuard;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (StringUtils.isEmpty((String) request.getAttribute("requestId"))) {
            String jwtToken = request.getHeader("Authorization");
            if (StringUtils.isNotEmpty(jwtToken)) {
                jwtToken = jwtToken.replace("Bearer ", "");

                try {
                    if (!securityGuard.isTokenValid(jwtToken)) {
                        handleAuthorizationError(response, "Invalid token");
                        return;
                    }
                }
                catch (ExpiredJwtException e) {
                    handleAuthorizationError(response, e.getMessage());
                    return;
                }
                Long userId = SecurityGuard.getUserId(jwtToken);

                Token token = new Token(SecurityGuard.getRequestId(), jwtToken);
                var userInfo = new UserInfo("", userId, token);

                String source = request.getHeader("source");
                if (StringUtils.isNotEmpty(source)) {
                    userInfo.setSource(source);
                }
                request.setAttribute("userInfo",userInfo);
                request.setAttribute("requestId", token.getRequestId());
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
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
}
