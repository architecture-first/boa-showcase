package com.architecture.first.framework.technical.threading.exceptions;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.vicinity.events.UnhandledExceptionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles uncaught exceptions and notifies an Actor
 */
@Slf4j
public class ThreadExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Autowired
    private Actor actor;

    /**
     * Handle an uncaught exception and notify the related Actor
     * @param t the thread
     * @param e the exception
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Error in thread: ", e);

        actor.notice(new UnhandledExceptionEvent(this, "vicinity", actor.name()));
    }
}
