package com.architecture.first.framework.business.vicinity.info;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Contains information for the particular Vicinity
 */
@Data
@Component
public class VicinityInfo {
    public static String VALUE_ENABLED = "enabled";
    public static String VALUE_DISABLED = "disabled";

    @Value("${vicinity.name}")
    private String name;

    @Value("${vicinity.to-do:enabled}")
    private String todo;

    @Value("${vicinity.acknowledgement:enabled}")
    private String acknowledgement;

    @Value("${vicinity.actor-entered-event:enabled}")
    private String actorEnteredEvent;

}
