package com.architecture.first.framework.technical.aop;

import com.architecture.first.framework.security.model.Token;
import com.architecture.first.framework.technical.user.UserInfo;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Holds context information for a reqeust
 */
@Data
@Component
@RequestScope
public class RequestContext {
    private UserInfo userInfo;
    private String requestId;
    private String tasklist;
    private boolean isAsync;
    private Throwable exception;
    private boolean hasErrors;

    public void setException(Throwable t) {
        this.exception = t;
        hasErrors = true;
    }

    public Long getCustomerId() {return (userInfo != null) ? userInfo.getUserId() : -1;}
    public Token getToken() {return (userInfo != null) ? userInfo.getToken() : new Token();}
    public String getAccessToken() {return getToken().getToken();}

    public String getRequestId() {
        return requestId;
    }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isAsync() {return isAsync;}

    public String getTasklist() {
        return tasklist;
    }

    public void setTasklist(String tasklist) {
        this.tasklist = tasklist;
    }
}
