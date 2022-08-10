package com.architecture.first.framework.technical.threading;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Manages threads for various processing
 */
@AllArgsConstructor
@Data
public class ThreadManagement {
    private ExecutorService executorService;
    private Future<?> future;

    /**
     * Graceful shut down
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Hard shut down
     * @return running tasks
     */
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    /**
     * Determines if the related threads are shut down
     * @return
     */
    public boolean isShutdown() {
        return executorService.isShutdown() || executorService.isTerminated();
    }
}
