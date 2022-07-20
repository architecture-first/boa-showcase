package com.architecture.first.framework.business.actors.external.behavior.script.model;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * The decompiled Logic script
 */
@Data
public class Script {
    private Map<String, String> define = new LinkedTreeMap<>();
    private Map<String, Object> declare = new LinkedTreeMap<>();
    private Map<String, PipelineStage> pipelineStageMap = new LinkedTreeMap<>();
    private ArchitectureFirstEvent initialEvent;
    private boolean isCompleted;
    private transient Gson gson = new Gson();

    public Script(ArchitectureFirstEvent event,
                  Map<String, String> definitions, Map<String, Object> declarations) {
        this.initialEvent = event;
        this.define = definitions;
        this.declare = declarations;
    }

    public ArchitectureFirstEvent getInitialEvent() {
        return initialEvent;
    }

    public void setInitialEvent(ArchitectureFirstEvent initialEvent) {
        this.initialEvent = initialEvent;
    }

    public Gson gson() {return gson;}
    public String actorName() {return initialEvent.getTarget().get().name();}
    public String eventFrom() {return initialEvent.from();}
    public String eventTo() {return initialEvent.toFirst();}

    public String getDefinition(String name) {return define.get(name);}
    public String getDeclaration(String name) {return (String) declare.get(name);}

    public void addStage(PipelineStage stage) {
        pipelineStageMap.put(String.valueOf(pipelineStageMap.size()), stage);
        if (pipelineStageMap.size() > 1) {  // copy previous output to input
            var previousStage = pipelineStageMap.get(String.valueOf(pipelineStageMap.size()-2));
            stage.setInput(previousStage.getOutput());
        }
    }

    public List<PipelineStage> getStages() {
        return pipelineStageMap.entrySet().stream().map(e -> e.getValue()).toList();
    }

    public boolean isCompleted() {return isCompleted;}
}
