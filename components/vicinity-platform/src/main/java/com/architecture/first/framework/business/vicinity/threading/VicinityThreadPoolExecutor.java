package com.architecture.first.framework.business.vicinity.threading;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manages threads used for Vicinity connections
 */
@Slf4j
public class VicinityThreadPoolExecutor extends ThreadPoolExecutor {

    public VicinityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    /**
     * Performs action after execution is completed due to an error
     * @param r the runnable that has completed
     * @param t the exception that caused termination, or null if execution completed normally
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if (t != null) {
            log.error("Error: ", t);
        }
    }
}
