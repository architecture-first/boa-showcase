package com.architecture.first.framework.business.vicinity.messages;

import com.google.gson.Gson;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * The message sent through the Vicinity between Actors
 */
@Data
@RequiredArgsConstructor
public class VicinityMessage implements Serializable {
    private VicinityHeader header = new VicinityHeader();
    private String jsonPayload = "";

    /**
     * Creates a Vicinity message
     * @param from - Actor sending the message
     * @param to - Actor to receive the message
     */
    public VicinityMessage(String from, String to) {
        header.setFrom(from);
        header.setTo(to);
    }

    /**
     * Sets the payload of the message by type
     * @param payload
     * @param classType
     * @return the message
     */
    public VicinityMessage setPayload(Object payload, Type classType) {
        header.setEventType(classType.getTypeName());
        jsonPayload = new Gson().toJson(payload, classType);

        return this;
    }

    /**
     * Returns who the message is from
     * @return
     */
    public String from() {
        return header.getFrom();
    }

    /**
     * Return who the message is targeted to
     * @return
     */
    public String to() {
        return header.getTo();
    }

    /**
     * Returns the token for the message
     * @return
     */
    public String token() {
        return header.getToken();
    }

    /**
     * Returns the subject of the message
     * @return
     */
    public String subject() {
        return header.getSubject();
    }

    /**
     * Returns this object as a JSON string
     * @return JSON string
     */
    public String toString() {
        return new Gson().toJson(this, this.getClass());
    }

    /**
     * Builds the Vicinity message from a JSON string
     * @param jsonMessage
     * @return
     */
    public static VicinityMessage from(String jsonMessage) {
        try {
            return new Gson().fromJson(jsonMessage, VicinityMessage.class);
        }
        catch (Exception e) {
            return null;
        }
    }

}
