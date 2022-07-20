package com.architecture.first.framework.business.actors.external.behavior.script;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A command to execute based on a time slot, such as "30min", "60min", "12hours", "24hours"
 */
@Data
public class on {
    // one or more of these may be null or not null based on the script
    @SerializedName("hear")
    private Map<String, Pipeline>   onHear;
    @SerializedName("30min")
    private Pipeline   on30min;
    @SerializedName("60min")
    private Pipeline   on60min;
    @SerializedName("12hours")
    private Pipeline on12hours;
    @SerializedName("24hours")
    private Pipeline on24hours;

    public List<String> getListeners() {
        if (onHear != null) {
            return onHear.keySet().stream().toList();
        }

        return new ArrayList<>();
    }

    public boolean eventListenersExist() {
        return onHear != null && onHear.size() > 0;
    }

    public boolean on30minListenerExists() {
        return on30min != null && on30min.size() > 0;
    }

    public boolean on60minListenerExists() {
        return on60min != null && on60min.size() > 0;
    }

    public boolean on12hoursListenerExists() {
        return on12hours != null && on12hours.size() > 0;
    }

    public boolean on24hoursListenerExists() {
        return on24hours != null && on24hours.size() > 0;
    }
}
