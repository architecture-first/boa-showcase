package com.architecture.first.framework.business.vicinity.tasklist;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a connection to a Tasklist
 */
@Data
@AllArgsConstructor
public class TasklistConnection {
    private String taskList;
    private String task;
}
