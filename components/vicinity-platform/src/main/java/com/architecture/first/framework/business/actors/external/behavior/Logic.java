package com.architecture.first.framework.business.actors.external.behavior;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.actors.exceptions.ActorException;
import com.architecture.first.framework.business.actors.external.behavior.script.PipelineContext;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Executes logic based on an external script.
 *
 * For example:
 * {
 *   "declare": {
 *     "brand-value": "'Ray-ban'"
 *   },
 *   "on": {
 *     "hear": {
 *       "AcquireCrossSellProducts": [
 *         {
 *           "$exec": {
 *             "whisper": {
 *               "name": "AcquireCrossSellProducts",
 *               "extern": true
 *             }
 *           }
 *         },
 *         {
 *           "$filter": "$.results[?(@.brand == {{brand-value}})]"
 *         }
 *       ]
 *     }
 *   }
 * }
 */
@Component
public class Logic {
    @Value("${vicinity.external.root-path:none}")
    private String rootPath;

    private PipelineContext pipelineContext;

    /**
     * Load logic from an external file
     * @param actor
     * @return a pipeline to execute
     */
    public Optional<PipelineContext> load(Actor actor) {
        if (rootPath.equals("none")) {
            return Optional.empty();
        }

        try {
            var name = actor.group();
            var standardName = name.toLowerCase();
            var filePath = String.format("%s/%s/%s-logic.json", rootPath, name, name);
            var path = Path.of(filePath);
            if (Files.exists(path)) {
                var contents = Files.readString(path);
                load(contents);

                return Optional.of(pipelineContext);
            }
        }
        catch (IOException e) {
            throw new ActorException(actor, e);
        }

        return Optional.empty();
    }

    /**
     * Load a pipeline context
     * @param jsonScript
     */
    public void load(String jsonScript) {
/*        RuntimeTypeAdapterFactory<Command> factory = RuntimeTypeAdapterFactory.of(Command.class, "type");
        factory.registerSubtype(exec.class, "$exec");
        factory.registerSubtype(filter.class, "$filter");*/
        pipelineContext = new Gson().fromJson(jsonScript, PipelineContext.class);
    }

    public List<String> events() {
        return null;
    }

    public boolean exists() {return pipelineContext != null && pipelineContext.on().eventListenersExist();}
    public boolean existsFor30min() {return pipelineContext != null && pipelineContext.on().on30minListenerExists();}
    public boolean existsFor60min() {return pipelineContext != null && pipelineContext.on().on60minListenerExists();}
    public boolean existsFor12hours() {return pipelineContext != null && pipelineContext.on().on12hoursListenerExists();}
    public boolean existsFor24hours() {return pipelineContext != null && pipelineContext.on().on24hoursListenerExists();}

    public List<String> getListeners() {
        return pipelineContext.getOn().getListeners();
    }

    /**
     * Execute a pipeline based on an event
     * @param event
     */
    public void apply(ArchitectureFirstEvent event) {
        var subject = event.subject();
        var pipeline = pipelineContext.getPipelineFor(subject);
        pipeline.execute(pipelineContext, event);
    }

    /**
     * Execute a pipeline on a given interval for an event
     * @param timeSlot - "30min", "60min", "12hr", "24hrs"
     * @param event
     */
    public void apply(String timeSlot, ArchitectureFirstEvent event) {
        var pipeline = pipelineContext.getPipelineFor(timeSlot);
        pipeline.execute(pipelineContext, event);
    }

    /**
     * Perform processing for an event
     */
    public static Function<ArchitectureFirstEvent, Actor> onExternalLogic = (event -> {
        var actor = event.getTarget().get();
        actor.onExternalLogic(event);

        return actor;
    });
}
