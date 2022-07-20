package com.architecture.first.framework.technical.aop;

import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.technical.user.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/**
 * Adds user information to the request context
 */
@RestControllerAdvice
public class UserAdvice extends RequestBodyAdviceAdapter {

    @Autowired
    private RequestContext requestContext;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return targetType.getTypeName().equals(UserInfo.class.getTypeName());
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        if (body instanceof UserInfo) {
            requestContext.setUserInfo((UserInfo) body);
            requestContext.setRequestId(SecurityGuard.getRequestId());
        }

        return body;
    }
}
