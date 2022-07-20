package com.architecture.first.framework.technical.user;

import com.architecture.first.framework.security.model.Token;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents user access information
 */
@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class UserInfo {
    private String source;
    private Long userId;
    private Token token;


    /**
     * Returns the user access token
     * @return
     */
    public String getAccessToken() {
        return token.getToken();
    }

    /**
     * Returns whether the user access token
     * @return
     */
    public boolean hasAccessToken() {
        return token != null && StringUtils.isNotEmpty(token.getToken());
    }

    /**
     * Returns the source actor
     * @return
     */
    public String getSourceActor() {return StringUtils.isNotEmpty(source) ? source : "Customer";}

    /**
     * Sets the access token
     * @param accessToken
     */
    public void setAccessToken(String accessToken) {
        if (token == null) {
            token = new Token();
        }

        token.setToken(accessToken);
    }

}
