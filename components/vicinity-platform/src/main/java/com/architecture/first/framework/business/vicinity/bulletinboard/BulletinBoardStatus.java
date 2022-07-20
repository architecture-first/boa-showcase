package com.architecture.first.framework.business.vicinity.bulletinboard;

import com.google.gson.Gson;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * A status entry for the bulletin board
 */
public class BulletinBoardStatus {
    public enum Status {
        Active ("Active"),
        Missing ("Missing"),
        Away ("Away"),
        Gone ("Gone");

        private final String status;

        Status(String status) {
            this.status = status;
        }
    }

    private BulletinBoardStatus.Status status;
    private final String subject;
    private String message;
    private String timestamp;

    public BulletinBoardStatus(BulletinBoardStatus.Status status, String subject, String message) {
        this.status = status;
        this.subject = subject;
        this.message = message;
        touch();
    }

    public BulletinBoardStatus(BulletinBoardStatus.Status status, String subject) {
        this(status, subject, "");
    }

    /**
     * Set the status
     * @param status
     */
    public void setStatus(BulletinBoardStatus.Status status) {
        this.status = status;
        touch();
    }

    /**
     * Set an optional message
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Update the timestamp
     */
    public void touch() {
        timestamp = ZonedDateTime.now(ZoneId.of("GMT")).toString();
    }

    /**
     * Get the current timestamp
     * @return
     */
    public ZonedDateTime getTimestamp() {
        try {
            return ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        }
        catch (Exception e) {  // ignore previous formats and force record to be updated
            return ZonedDateTime.now(ZoneId.of("GMT")).minus(3, ChronoUnit.MINUTES);
        }
    }

    /**
     * Format the entry for recording
     * @return
     */
    private String formatEntry() {
        return new Gson().toJson(this, BulletinBoardStatus.class);
    }

    /**
     * Returns the object as JSON
     * @return JSON string
     */
    public String toString() {
        return formatEntry();
    }

    /**
     * Create a status object from JSON
     * @param json
     * @return
     */
    public static BulletinBoardStatus from(String json) {
        return new Gson().fromJson(json, BulletinBoardStatus.class);
    }
}



