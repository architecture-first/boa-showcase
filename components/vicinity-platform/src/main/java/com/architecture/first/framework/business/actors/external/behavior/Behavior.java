package com.architecture.first.framework.business.actors.external.behavior;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.actors.exceptions.ActorException;
import com.architecture.first.framework.business.actors.external.RestCall;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Perform external calls based on a script.
 *
 * For example:
 *   "AcquireCrossSellProducts": {
 *     "headers": [],
 *     "method": "GET",
 *     "uri": "http://localhost:3001/advertiser-service/api/crosssells",
 *     "params": ["type=Sunglasses"],
 *     "body": {}
 *   }
 */
@Component
public class Behavior {
    @Value("${vicinity.external.root-path:none}")
    private String rootPath;

    private Map<String, RestCall> behaviors = new HashMap<>();

    /**
     * Load the external behavior from a file
     * @param actor
     * @return
     */
    public Optional<Map<String, RestCall>> getBehaviors(Actor actor) {
        if (rootPath.equals("none")) {
            return Optional.empty();
        }

        try {
            var name = actor.group();
            var standardName = name.toLowerCase();
            var filePath = String.format("%s/%s/%s-calls.json", rootPath, name, name);
            var path = Path.of(filePath);
            if (Files.exists(path)) {
                var contents = Files.readString(path);

                Type listType = new TypeToken<Map<String, RestCall>>() {}.getType();
                Map<String, RestCall> calls = new Gson().fromJson(contents, listType);
                behaviors = calls;

                return Optional.of(calls);
            }
        }
        catch (IOException e) {
            throw new ActorException(actor, e);
        }

        return Optional.empty();
    }

    /**
     * Determines if a given behavior exists
     * @param name - the event name to check
     * @return true if the event behavior exists
     */
    public boolean contains(String name) {
        return behaviors.containsKey(name.replace("Event",""));
    }

    /**
     * Determines if the behavior supports a given event
     * @param event - the event to check
     * @return true if the event behavior exists
     */
    public boolean contains(ArchitectureFirstEvent event) {
        return contains(event.name());
    }

    /**
     * Performs the handling of an event
     * @param event - the event to handle
     * @return the results of the event
     */
    public Optional<Object>  perform(ArchitectureFirstEvent event) {
        return perform(event, Object.class);
    }

    /**
     * Performs the handling of an event by type
     * @param event - the event to handle
     * @param classType - the class type of the event
     * @return the results of the event
     */
    public Optional<Object> perform(ArchitectureFirstEvent event, Type classType) {
        var key = event.name().replace("Event", "");
        if (behaviors.containsKey(key)) {
            var restCall = behaviors.get(key);

            var response = restCall.execute(event.header(), event.payload());

            return Optional.of(new Gson().fromJson(response.body(), classType));
         }

        return Optional.empty();
    }

    /**
     * Handle the external behavior
     */
    public static Function<ArchitectureFirstEvent, Actor> onExternalBehavior = (event -> {
        var actor = event.getTarget().get();
        actor.onExternalBehavior(event);

        return actor;
    });
}
