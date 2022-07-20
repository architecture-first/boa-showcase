package com.architecture.first.framework.business.actors.external.behavior.script;

import com.architecture.first.framework.business.actors.external.behavior.script.model.PipelineEntry;
import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A command to filter data based on criteria
 */
@Data
public class filter extends Command {
    private String field;
    private transient Map<String, PipelineEntry> input;
    private transient Map<String, PipelineEntry> output;

    @Override
    public boolean execute(Map<String, PipelineEntry> input,
                                          Map<String, PipelineEntry> output) {
        this.input = input;
        this.output = output;

        // $.results[?(@.brand == 'Ray-Ban')]
        // $.[?(@.brand == 'Ray-Ban')]
        AtomicReference<String> filter = new AtomicReference<>("$.results[?(@.brand == {{brand-value}})]");
        getScript().getDeclare().entrySet().forEach(e -> {
            var token = String.format("{{%s}}", e.getKey());
            filter.set(filter.get().replace(token, (String) e.getValue()));
        });

        var end = filter.get();

        var json = new Gson().toJson(input);
        var results = JsonPath.read(json, "$.results.data[?(@.brand == 'Ray-Ban')]");
        var entry = new PipelineEntry(results.getClass().getName(), results);
        output.put("results", entry);

        return true;
    }
}
