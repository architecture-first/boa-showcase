package com.architecture.first.framework.business.retail.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

public class PaymentResponseEvent extends ArchitectureFirstEvent {
    public PaymentResponseEvent(Object source, String from, String to) {
        super(source, from, to);
    }

    public Long getCustomerId() {
        return (Long) this.header().get("userId");
    }

    public PaymentResponseEvent setCustomerId(Long customerId) {
        this.header().put("userId", customerId); return this;
    }

    public Long getOrderNumber() {
        return (Long) this.payload().get("orderNumber");
    }

    public PaymentResponseEvent setOrderNumber(Long customerId) {
        this.payload().put("orderNumber", customerId); return this;
    }

    public Boolean getApprovalStatus() {
        return (Boolean) payload().get("approvalStatus");
    }

    public PaymentResponseEvent setApprovalStatus(Boolean approvalStatus) {
        this.payload().put("approvalStatus", approvalStatus);
        return this;
    }

    @Override
    public void onVicinityInit() {
        if (this.header().get("userId") instanceof Double) {
            setCustomerId(((Double) this.header().get("userId")).longValue());     // Convert Gson default data type
        }
        if (this.payload().get("orderNumber") instanceof Double) {
            setOrderNumber(((Double) this.payload().get("orderNumber")).longValue());     // Convert Gson default data type
        }
    }
}
