package com.architecture.first.framework.business.actors.external.behavior.script;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import lombok.Data;

import java.util.Map;

/**
 * The context to run the dynamic Logic in
 */
@Data
public class PipelineContext {
    private Map<String, String> define = new LinkedTreeMap<>();
    private Map<String, Object> declare = new LinkedTreeMap<>();
    private on on;
    private transient Gson gson = new Gson();

    public Pipeline getPipelineFor(String subject) {
        switch (subject) {
            case "30min":
                return on.getOn30min();
            case "60min":
                return on.getOn60min();
            case "12hours":
                return on.getOn12hours();
            case "24hours":
                return on.getOn24hours();
        }
        return on.getOnHear().get(subject);
    }
    public Gson gson() {return gson;}
    public on on() {return on;}

    public String getDefinition(String name) { return define.get(name); }
    public Object getDeclaration(String name) {return declare.get(name);}
}
