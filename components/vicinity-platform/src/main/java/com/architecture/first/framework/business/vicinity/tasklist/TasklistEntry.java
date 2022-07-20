package com.architecture.first.framework.business.vicinity.tasklist;

import com.google.gson.Gson;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Represents an entry in a task list
 */
public class TasklistEntry {
    public enum Status {
        Pending ("Pending"),
        InProgress ("InProgress"),
        Complete ("Complete"),
        Failed ("Failed"),
        Gone ("Gone");

        private final String status;

        Status(String status) {
            this.status = status;
        }
    }

    private TasklistEntry.Status status;
    private String message;
    private String timeStamp;

    public TasklistEntry(TasklistEntry.Status status, String message) {
        this.status = status;
        this.message = message;
        touch();
    }

    public void setStatus(TasklistEntry.Status status) {
        this.status = status;
        touch();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void touch() {
        timeStamp = ZonedDateTime.now(ZoneId.of("GMT")).toString();
    }

    private String formatEntry() {
        return new Gson().toJson(this, TasklistEntry.class);
    }

    public String toString() {
        return formatEntry();
    }
}



