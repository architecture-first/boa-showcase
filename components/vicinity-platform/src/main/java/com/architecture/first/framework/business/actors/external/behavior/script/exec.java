package com.architecture.first.framework.business.actors.external.behavior.script;

import com.architecture.first.framework.business.actors.external.behavior.script.model.PipelineEntry;
import com.architecture.first.framework.business.vicinity.events.AcknowledgementEvent;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.util.SimpleModel;
import com.google.gson.Gson;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A command to communicate in various ways
 */
@Slf4j
@Data
public class exec extends Command {
    private call call;
    private say say;
    private whisper whisper;
    private announce announce;
    private String eventName;
    private transient Map<String, PipelineEntry> input;
    private transient Map<String, PipelineEntry> output;

    @Override
    public boolean execute(Map<String, PipelineEntry> input,
                                          Map<String, PipelineEntry> output) {
        this.input = input;
        this.output = output;

        if (call != null) {
            return executeCall();
        }
        if (say != null) {
            return executeSay();
        }
        if (whisper != null) {
            return executeWhisper();
        }
        if (announce != null) {
            return executeAnnounce();
        }
        return false;
    }

    private boolean executeCall() {
        var actor = getScript().getInitialEvent().getTarget().get();
        ArchitectureFirstEvent event = generateEvent(call);
        processResults(event);

        actor.onExternalBehavior(event);
        return true;
    }

    private boolean executeSay() {
        var actor = getScript().getInitialEvent().getTarget().get();
        ArchitectureFirstEvent event = generateEvent(say);
        event.setName(getScript().getInitialEvent().name());
        processResults(event);

        var evt = (say.isAwaitResponse())
                            ? actor.say(event, response -> {
                                    if (!(event instanceof AcknowledgementEvent)) {
                                        if (event.payload() != null) {
                                            var entry = (event.payload().containsKey("results") && (event.payload().get("results") instanceof PipelineEntry))
                                                    ? (PipelineEntry) event.payload().get("results")
                                                    : new PipelineEntry(event.payload().getClass().getName(), event.payload());
                                            output.put("results", entry);
                                        }
                                    }

                                    return true;
                                })
                            : actor.say(event);

        return true;
    }

    private void processResults(ArchitectureFirstEvent event) {
        AtomicReference<List<Object>> aList = new AtomicReference<>();
        AtomicReference<String> aKey = new AtomicReference<>();

        if (event.payload() != null) {  // payload is optional
            event.payload().entrySet().stream()
                    .filter(e -> "$$pipeline.results".equals(e.getValue()))
                    .forEach(e -> {
                        aKey.set(e.getKey());
                        var data = input.get("results").getData();
                        aList.set((data instanceof SimpleModel)
                                ? ((SimpleModel) data).entrySet().stream().collect(Collectors.toList())
                                : ((JSONArray) data).stream().toList()
                        );
                    });

            if (aList.get() != null) {
                var results = new ArrayList<>();
                results.addAll(aList.get());
                event.payload().put(aKey.get(), aList.get());
            }
        }
    }

    private boolean executeWhisper() {
        var actor = getScript().getInitialEvent().getTarget().get();
        ArchitectureFirstEvent results;
        if (whisper.isExtern()) {
            results = actor.onExternalBehavior(getScript().getInitialEvent());
            output.put("results", (PipelineEntry) results.payload().get("results"));
            return true;
        }

        ArchitectureFirstEvent event = generateEvent(whisper);
        processResults(event);

        results = actor.whisper(event);
        output.put("results", (PipelineEntry) event.payload().get("results"));

        return true;
    }

    private boolean executeAnnounce() {
        var actor = getScript().getInitialEvent().getTarget().get();
        ArchitectureFirstEvent event = generateEvent(announce);
        processResults(event);

        actor.announce(event);

        return false;
    }

    private ArchitectureFirstEvent generateEvent(say communication) {
        if (StringUtils.isEmpty(communication.getFrom())) {
            communication.setFrom(getScript().eventTo()); // send from self since received it
        }
        if (StringUtils.isEmpty(communication.getTo())) {
            communication.setTo(getScript().eventTo());
            log.warn("to is not defined");
        }

        var qualifiedName = getScript().getDefinition(communication.getName());
        var cls = getClass(qualifiedName);
        var event = (ArchitectureFirstEvent) getInstanceFromScript(communication, communication.getClass(), cls);

        event.setOriginalEvent(getScript().getInitialEvent());

        if (StringUtils.isNotEmpty(communication.getFrom())) {
            event.setFrom(communication.getFrom());
        }
        if (StringUtils.isNotEmpty(communication.getTo())) {
            event.setTo(communication.getTo());
        }
        if (event.source() == null) {
            event.setSource("exec Script");
        }
        event.resetTargetActor();
        event.setAsPipelineEvent(true);

        if (communication.isAwaitResponse()) {
            event.setAwaitTimeoutSeconds(30);
        }

        return event;
    }

    private Object getInstanceFromScript(Object command, Type commandType, Type eventType) {
        var json = new Gson().toJson(command, commandType);
        var obj = new Gson().fromJson(json, eventType);

        return obj;
    }

    private Object getInstanceFromScratch(String name, String from, String to) {
        try {
            var cls = Class.forName(name);
            return cls.getDeclaredConstructor(Object.class, String.class, String.class)
                    .newInstance(this, from, to);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        }
        catch (Exception e) {
            throw new RuntimeException("Class not found for name: " + name, e);
        }
    }
}
