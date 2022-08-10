package com.architecture.first.framework.technical.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Represents a Connection to an asynchronous thread and context
 */
public class Connection extends ThreadManagement {

    public Connection(ExecutorService executorService, Future<?> future) {
        super(executorService, future);
    }

}
