package com.architecture.first.framework.business.retail.model.customer;

import com.architecture.first.framework.security.model.Token;
import com.architecture.first.framework.technical.user.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class CustomerInfo extends UserInfo {
    public CustomerInfo(String source, Long userId, Token token) {
        super(source, userId, token);
    }

    public CustomerInfo() {
    }
}
