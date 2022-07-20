package com.architecture.first.framework.business.actors.external.behavior.script;

import com.architecture.first.framework.technical.util.SimpleModel;
import lombok.Data;

/**
 * A command to say an event to another Actor in the Vicinity
 */
@Data
public class say {
    private String name;
    private boolean extern;
    private boolean awaitResponse = true;
    private long awaitTimeoutSeconds = 30;
    private boolean isLocal;
    private String to;
    private String from;
    private transient SimpleModel headers;
    private SimpleModel payload;
}
