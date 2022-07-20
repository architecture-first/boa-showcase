package com.architecture.first.framework.business.actors.external.behavior.script.model;

import com.architecture.first.framework.business.actors.external.behavior.script.Command;
import com.architecture.first.framework.business.actors.external.behavior.script.exec;
import com.architecture.first.framework.business.actors.external.behavior.script.filter;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.google.gson.internal.LinkedTreeMap;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * A stage in the pipeline for dynamic logic
 */
@Data
public class PipelineStage {
    private Map<String, Object> commandBlock = new LinkedTreeMap<>();
    private Map<String, PipelineEntry> input = new LinkedTreeMap<>();
    private Map<String, PipelineEntry> output = new LinkedTreeMap<>();
    private int index;

    public PipelineStage(String name, ArchitectureFirstEvent event) {
        setEvent(name, event);
    }

    public PipelineStage(int index, Map<String, Object> commandBlock) {
        this.commandBlock = commandBlock;
        this.index = index;
    }

    public PipelineStage setEvent(String name, ArchitectureFirstEvent event) {
        var entry = new PipelineEntry(event.getClass().getName(), event);
        input.put(name, entry);
        return this;
    }

    public boolean execute(Script script) {
        var cmd = getCommand(script);
        return cmd.execute(input, output);
    }

    private Command getCommand(Script script) {
        var commandName = commandBlock.keySet().stream().toList().get(0);
        switch (commandName) {
            case "$exec":
                return generateCommand(script, commandName, exec.class);
            case "$filter":
                filter flter = new filter();
                flter.setField((String) commandBlock.get(commandName));
                flter.setScript(script);
                return flter;
        }
        return null;
    }

    private Command generateCommand(Script script, String commandName, Type classType) {
        var jsonData = script.gson().toJson(commandBlock.get(commandName));
        Command cmd = script.gson().fromJson(jsonData, classType);
        cmd.setScript(script);

        return cmd;
    }
}
