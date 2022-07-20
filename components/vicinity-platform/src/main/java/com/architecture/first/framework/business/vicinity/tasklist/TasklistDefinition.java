package com.architecture.first.framework.business.vicinity.tasklist;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a task list definition
 */
@Data
public class TasklistDefinition {
    private String name;
    private List<String> tasks = new ArrayList<>();
}
