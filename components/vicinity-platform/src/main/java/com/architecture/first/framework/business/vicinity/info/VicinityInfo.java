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

    public VicinityInfo() {}

    public VicinityInfo(VicinityInfo info) {
        from(info);
    }

    public void from(VicinityInfo info) {
        this.name = info.name;
        this.todo = info.todo;
        this.acknowledgement = info.acknowledgement;
        this.actorEnteredEvent = info.actorEnteredEvent;
    }

    @Value("${vicinity.name:local-vicinity}")
    private String name;

    @Value("${vicinity.env.to-do:disabled}")
    private String todo;

    @Value("${vicinity.env.acknowledgement:disabled}")
    private String acknowledgement;

    @Value("${vicinity.env.actor-entered-event:disabled}")
    private String actorEnteredEvent;

}
