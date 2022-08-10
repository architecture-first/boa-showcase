package com.architecture.first.framework.technical.util;

import java.lang.management.ManagementFactory;

/**
 * Utility runtime methods
 */
public class RuntimeUtils {

    /**
     * Determines if the code is running in a debugger
     * @return
     */
    public static boolean isInDebugger() {
        var numDebuggers = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(arg -> arg.contains("jdwp=")).count();
        return numDebuggers > 0;
    }
}
