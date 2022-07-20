package com.architecture.first.framework.business.actors.external.behavior.script.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * An entry for Logic processing
 */
@AllArgsConstructor
@Data
public class PipelineEntry {
    private String dataType;
    private Object data;
}
