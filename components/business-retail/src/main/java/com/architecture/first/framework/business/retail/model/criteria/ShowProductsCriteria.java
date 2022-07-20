package com.architecture.first.framework.business.retail.model.criteria;

import com.architecture.first.framework.business.retail.storefront.model.ICriteria;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class ShowProductsCriteria implements ICriteria {
    private String jsonCriteria = "";

    public ShowProductsCriteria() {
    }

    public ShowProductsCriteria(String criteria) {
        jsonCriteria = criteria;
    }

    public void setJsonCriteria(String jsonCriteria) {
        this.jsonCriteria = jsonCriteria;
    }

    public boolean isEmpty() {
        return "".equals(jsonCriteria) || "{}".equals(jsonCriteria);
    }

    @Override
    public String toString() {
        return StringUtils.isNotEmpty(jsonCriteria) ? jsonCriteria : "{}";
    }
}
