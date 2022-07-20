package com.architecture.first.framework.business.actors.external.behavior.script;

/**
 * A command to whisper to self or another Actor in the same process.
 */
public class whisper extends say {
    public whisper() {
        super();
        this.setLocal(true);
    }
}
