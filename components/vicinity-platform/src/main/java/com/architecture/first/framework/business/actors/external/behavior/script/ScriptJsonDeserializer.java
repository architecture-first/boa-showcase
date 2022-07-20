package com.architecture.first.framework.business.actors.external.behavior.script;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class ScriptJsonDeserializer extends JsonDeserializer<PipelineContext> {
    @Override
    public PipelineContext deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return null; // TODO - implement if necessary
    }

}
