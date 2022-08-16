package com.architecture.first.framework.security.model;

import java.util.Date;

/**
 * System-wide technical information
 */
public class SystemInfo {
    private boolean hasErrors;
    private String errorCode = "";
    private String message = "";
//    private Date updateDate;
    private String updatedBy;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean hasErrors() {
        return this.hasErrors;
    }

    public void hasErrors(boolean status) {
        this.hasErrors = status;
    }

/*    public Date getUpdateDate() {
        return updateDate;
    }*/

/*    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }*/

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
