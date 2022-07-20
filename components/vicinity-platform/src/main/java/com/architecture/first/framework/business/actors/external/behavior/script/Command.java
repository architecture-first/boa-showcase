package com.architecture.first.framework.business.actors.external.behavior.script;

import com.architecture.first.framework.business.actors.external.behavior.script.model.PipelineEntry;
import com.architecture.first.framework.business.actors.external.behavior.script.model.Script;
import lombok.Data;

import java.util.Map;

/**
 * Execute a command based on a script
 */
@Data
public abstract class Command {
    private transient Script script;

    public abstract boolean execute(Map<String, PipelineEntry> input,
                                                   Map<String, PipelineEntry> output);

    public Script getScript() {
        return script;
    }

    public Command setScript(Script script) {
        this.script = script;
        return this;
    }
}
