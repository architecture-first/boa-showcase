package com.architecture.first.framework.business.actors.external.behavior.script;

import com.architecture.first.framework.business.actors.external.behavior.script.model.PipelineStage;
import com.architecture.first.framework.business.actors.external.behavior.script.model.Script;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;

import java.util.ArrayList;
import java.util.Map;

/**
 * The pipeline to execute dynamic Logic
 */
public class Pipeline extends ArrayList<Map<String,Object>> {

    public void execute(PipelineContext context, ArchitectureFirstEvent event) {
        Script script = new Script(event, context.getDefine(), context.getDeclare());
        execute(script);
    }

    public void execute(Script script) {
        var iter = this.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            var commandBlock = iter.next();
            var stage = new PipelineStage(i, commandBlock);
            script.addStage(stage);

            if (!stage.execute(script)) {
                break;
            }
        }
    }
}
